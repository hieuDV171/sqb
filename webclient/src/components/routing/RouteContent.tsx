import { Component, Suspense, type ReactNode } from 'react';
import { ErrorState } from '@/components/ui/error-state';
import { Skeleton } from '@/components/ui/skeleton';

interface RouteContentProps {
  children: ReactNode;
  routeKey: string;
}

interface RouteContentState {
  routeKey: string;
  hasError: boolean;
}

export class RouteContent extends Component<RouteContentProps, RouteContentState> {
  state: RouteContentState = { routeKey: this.props.routeKey, hasError: false };

  static getDerivedStateFromProps(props: RouteContentProps, state: RouteContentState) {
    return props.routeKey === state.routeKey ? null : { routeKey: props.routeKey, hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div role="alert">
          <ErrorState
            title="Không thể mở trang"
            message="Hãy thử tải lại trang."
            onRetry={() => window.location.reload()}
          />
        </div>
      );
    }

    return (
      <Suspense fallback={
        <div role="status" className="space-y-4">
          <span className="sr-only">Đang tải trang</span>
          <Skeleton className="h-8 w-48 motion-reduce:animate-none" aria-hidden="true" />
          <Skeleton className="h-64 w-full rounded-2xl motion-reduce:animate-none" aria-hidden="true" />
        </div>
      }>
        {this.props.children}
      </Suspense>
    );
  }
}
