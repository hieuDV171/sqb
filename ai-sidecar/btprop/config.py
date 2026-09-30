import math
from dataclasses import dataclass, asdict

VERSION = "sqb-btprop-1.0.0"
PROMPT_VERSION = "sqb-multi-subject-2026-09-30-v1"
LOGIC_VERSION = "2026-09-30-multi-answer-r1"
CORROBORATION_RISK = 50.0

@dataclass(frozen=True)
class Config:
    model: str = "gpt-4.1-2025-04-14"
    depth: int = 2          # Số cạnh tối đa từ gốc tới lá
    branches: int = 3       # Số nhánh mở rộng tối đa
    max_decomposition: int = 16  # Vượt giới hạn => fallback, không cắt mệnh đề
    max_nodes: int = 120    # Mỗi cây tối đa 120 nút
    workers: int = 2
    max_attempts: int = 3
    low_threshold: float = 30.0
    high_threshold: float = 70.0
    bins: tuple = (0.19, 0.39, 0.69, 0.89)
    # Xác suất quan sát (Emission) từ benchmark thực nghiệm upstream BTProp (NAACL 2025)
    emission_true: tuple = (
        0.11904761904761904, 
        0.051587301587301584,
        0.0992063492063492, 
        0.08333333333333333, 
        0.6468253968253969
    )
    emission_false: tuple = (
        0.29714285714285715, 
        0.09571428571428571,
        0.15571428571428572, 
        0.13428571428571429, 
        0.3171428571428571
    )
    prior_true: float = 0.5

    def __post_init__(self):
        if not 0 <= self.depth <= 4:
            raise ValueError("depth phải từ 0 đến 4")
        if self.branches < 1 or self.max_nodes < 1:
            raise ValueError("Giới hạn cây không hợp lệ")
        if not 0 < self.prior_true < 1 or not 0 < self.low_threshold < self.high_threshold < 100:
            raise ValueError("Prior/ngưỡng không hợp lệ")
        for row in (self.emission_true, self.emission_false):
            if len(row) != len(self.bins) + 1 or any(x <= 0 for x in row) or not math.isclose(sum(row), 1, abs_tol=1e-6):
                raise ValueError("Emission phải là phân phối dương có tổng bằng 1")

NEG_INF = float("-inf")

# Ma trận chuyển trạng thái logic (Transition Matrices)
# Hàng = parent False, True; Cột = child False, True
TRANSITIONS = {
    "equivalence": ((1.0, 0.0), (0.0, 1.0)),
    "entailment": ((0.5, 0.5), (0.0, 1.0)),
    "reverse_entailment": ((1.0, 0.0), (0.5, 0.5)),
    "contradiction": ((0.5, 0.5), (1.0, 0.0)),
}

def map_relation(forward: str, reverse: str) -> str:
    if "contradiction" in (forward, reverse):
        return "contradiction"
    return {
        ("entailment", "entailment"): "equivalence",
        ("entailment", "neutral"): "entailment",
        ("neutral", "entailment"): "reverse_entailment",
        ("neutral", "neutral"): "neutral"
    }.get((forward, reverse), "neutral")
