import { useState } from 'react';

export function useSessionFormError() {
  const [issue, setIssue] = useState<{ target: string; message: string }>();

  function show(target: string, message: string, focus = false) {
    setIssue({ target, message });
    if (focus) document.getElementById(target)?.focus();
  }

  function clear(...targets: string[]) {
    setIssue((current) => targets.length === 0 || (current && targets.includes(current.target)) ? undefined : current);
  }

  function messageAt(target: string) {
    return issue?.target === target ? issue.message : undefined;
  }

  function inputProps(target: string) {
    return {
      'aria-invalid': issue?.target === target || undefined,
      'aria-describedby': issue?.target === target ? `${target}-error` : undefined,
    };
  }

  return { show, clear, messageAt, inputProps };
}
