// src/services/matchSocketService.ts
import type { MatchMessage } from './MatchMessage';

export class MatchSocketService {
    private socket: WebSocket | null = null;
    private onMessageCallback?: (msg: MatchMessage) => void;
    private onBinaryCallback?: (data: Uint8Array) => void;

    constructor(private readonly url: string) { }

    public connect(): Promise<void> {
        return new Promise((resolve, reject) => {
            this.socket = new WebSocket(this.url);
            this.socket.binaryType = 'arraybuffer';

            this.socket.onopen = () => resolve();
            this.socket.onerror = (err) => reject(err);

            this.socket.onmessage = (event) => {
                // 배포 환경에서 실제 데이터 타입 확인용 로그
    console.log("[MatchWS] 수신 데이터 타입:", event.data.constructor.name);
    
                if (event.data instanceof ArrayBuffer) {
                    this.onBinaryCallback?.(new Uint8Array(event.data));
                    return;
                }
                try {
                    const data = JSON.parse(event.data) as MatchMessage;
                    this.onMessageCallback?.(data);
                } catch (e) {
                    console.error("[MatchWS] 파싱 에러:", e);
                }
            };
        });
    }

    // 외부(Composable)에서 수신 리스너를 등록할 수 있게 합니다.
    public setMessageHandler(cb: (msg: MatchMessage) => void) { this.onMessageCallback = cb; }
    public setBinaryHandler(cb: (data: Uint8Array) => void) { this.onBinaryCallback = cb; }

    public send(data: unknown): void {
        if (this.socket?.readyState === WebSocket.OPEN) {
            this.socket.send(
                typeof data === 'string' ? data : JSON.stringify(data)
            );
        }
    }

    public sendBinary(data: Uint8Array): void {
        if (this.socket?.readyState === WebSocket.OPEN)
            this.socket.send(data);
    }

    public disconnect(): void {
        this.socket?.close();
        this.socket = null;
    }
}
