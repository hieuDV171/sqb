import os
import unittest
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from btprop import btprop_router
from btprop.backend import EvaluationError
from btprop.config import Config
from btprop.engine import evaluate_question
from btprop.schemas import QuestionAuditRequest


def question(**overrides):
    return {
        "content": "Which number equals two plus two?",
        "options": [
            {"key": "A", "text": "Four", "is_correct": True},
            {"key": "B", "text": "Five", "is_correct": False},
        ],
        "explanation": "Two plus two equals four.",
        **overrides,
    }


class FakeModel:
    mode = "fake"
    missing_quote = False
    partial_failure = False

    def __init__(self, **_):
        self.calls = []

    def confidence(self, statement):
        self.calls.append(("confidence", statement))
        if self.partial_failure and statement.startswith("Five"):
            raise EvaluationError("Model timed out")
        return 0.01 if statement.startswith(("Five", "2 + 2 = 5")) else 0.99

    def json(self, task, data):
        self.calls.append((task, data))
        if task == "analyze":
            assert "explanation" not in data
            assert all("is_correct" not in item for item in data["options"])
            return {"options": [{"key": item["key"], "statement": item["text"] + " is suitable"}
                                for item in data["options"]],
                    "premises": [], "ambiguities": [], "coverage_complete": True}
        if task == "explanation":
            quote = "Fabricated quote" if self.missing_quote else data["explanation"]
            return {"claims": [{"statement": data["explanation"], "source_quote": quote}],
                    "coverage_complete": True}
        if task == "decompose":
            statement = data["statement"]
            return {"claims": [statement + " component one", statement + " component two"],
                    "complete": True, "joint_equivalence": True}
        raise AssertionError(task)


class BtpropAuditTests(unittest.TestCase):
    def setUp(self):
        app = FastAPI()
        app.include_router(btprop_router)
        self.client = TestClient(app)

    @patch.dict(os.environ, {"OPENAI_API_KEY": "mock-only", "BTPROP_DEPTH": "2"})
    @patch("btprop.router.OpenAIBackend", new=FakeModel)
    def test_full_http_audit_expands_two_levels_and_does_not_report_no_answer(self):
        response = self.client.post("/audit", json=question())
        self.assertEqual(200, response.status_code)
        audit = response.json()
        self.assertEqual("complete", audit["status"])
        self.assertEqual(2, audit["belief_tree_depth"])
        self.assertEqual(["A"], audit["likely_suitable_options"])
        self.assertNotIn("NO_ANSWER", [item["type"] for item in audit["violations"]])

    def test_audit_with_shallow_tree_does_not_infer_no_answer_from_empty_suitable_set(self):
        audit = evaluate_question(QuestionAuditRequest(**question()), FakeModel(), Config(depth=0))
        self.assertEqual([], audit.likely_suitable_options)
        self.assertNotIn("NO_ANSWER", [item.type for item in audit.violations])

    def test_partial_failure_has_no_overall_confidence(self):
        backend = FakeModel()
        backend.partial_failure = True
        audit = evaluate_question(QuestionAuditRequest(**question()), backend, Config(depth=1))
        self.assertEqual("incomplete", audit.status)
        self.assertIsNone(audit.confidence_score)
        self.assertIsNone(audit.risk_score)
        self.assertFalse(audit.is_hallucinated)

    def test_explanation_requires_a_quote_from_submitted_explanation(self):
        backend = FakeModel()
        backend.missing_quote = True
        audit = evaluate_question(QuestionAuditRequest(**question()), backend, Config(depth=0))
        self.assertEqual("incomplete", audit.status)
        self.assertIsNone(audit.risk_score)

    def test_false_statement_is_checked_as_a_claim(self):
        backend = FakeModel()
        request = QuestionAuditRequest(**question(content="2 + 2 = 5.", options=[
            {"key": "A", "text": "Đúng", "is_correct": False},
            {"key": "B", "text": "Sai", "is_correct": True},
        ]))
        audit = evaluate_question(request, backend, Config(depth=0))
        self.assertEqual("complete", audit.status)
        self.assertIn(("confidence", "2 + 2 = 5."), backend.calls)

    @patch.dict(os.environ, {"OPENAI_API_KEY": ""})
    def test_missing_key_is_unverified_with_no_scores(self):
        response = self.client.post("/audit", json=question())
        self.assertEqual(200, response.status_code)
        self.assertEqual("unverified", response.json()["status"])
        self.assertIsNone(response.json()["confidence_score"])
