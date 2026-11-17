export function getErrorMessage(error: unknown): string | undefined {
  if (typeof error === 'object' && error !== null && 'message' in error && typeof error.message === 'string') {
    return error.message || undefined;
  }
}

function getErrorResponse(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const { response } = error;
    if (typeof response === 'object' && response !== null) return response;
  }
}

export function getResponseErrorMessage(error: unknown): string | undefined {
  const response = getErrorResponse(error);
  return response && 'data' in response ? getErrorMessage(response.data) : undefined;
}

export function getHttpStatus(error: unknown): number | undefined {
  const response = getErrorResponse(error);
  if (response && 'status' in response && typeof response.status === 'number' && response.status !== 0) {
    return response.status;
  }
  if (typeof error === 'object' && error !== null && 'status' in error && typeof error.status === 'number') {
    return error.status;
  }
}
