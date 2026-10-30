package com.tenco.spring_blog.refund;

import com.tenco.spring_blog._core.util.MyDateUtil;
import lombok.Builder;
import lombok.Data;

public class RefundResponse {

    @Data
    public static class ListDto {
        private Long id;        // 환불 요청 pk
        private int amount;     // 환불 요청 금액
        private String paymentId;   // 포트원 결제 건 식별자 (주문번호)
        private String reason; // 본인이 작성한 환불 사유
        private String rejectReason; // 거절 사유 (거절될일 때만 값이 존재 - 관리자)
        private String statusDisplay; // 대기중, 승인됨, 거절됨
        private String requestedAt; // 환불 요청일

        // 머스태치 조건 분기용 (상태별 배지 색)
        private boolean pending;
        private boolean approved;
        private boolean rejected;

        @Builder
        public ListDto(Refund refund) {
            this.id = refund.getId();
            this.amount = refund.getPayment().getAmount();
            this.paymentId = refund.getPayment().getPaymentId(); // 주문 번호
            this.reason = refund.getReason();
            if (refund.getReason() != null) {
                this.rejectReason = refund.getRejectReason();
            }
            this.statusDisplay = refund.getStatus().getDisplayName(); // enum("결제 완료")
            if (refund.getCreatedAt() != null) {
                this.requestedAt = MyDateUtil.timestampFormat(refund.getCreatedAt());
            }
            this.pending = refund.getStatus() == RefundStatus.PENDING;
            this.approved = refund.getStatus() == RefundStatus.APPROVED;
            this.rejected = refund.getStatus() == RefundStatus.REJECTED;

        }
    }

    public static class AdminListDto {
        private Long id; // 환불 요청 PK
        private String username; // 환불 신청자 이름
        private String paymentId; // 포트원 결제 건 식별자
        private int amount; // 환불 금액
        private String reason; // 신청자 환불 사유
        private String statusDisplay; // 대기중, 승인됨, 거절됨
        private String requestedAt; // 환불 신청일

        private boolean pending; // 대기중 여부 (모달 버튼 , 배지색)
        private boolean approved; // 승인됨 여부

        public AdminListDto(Refund refund) {
            this.id = refund.getId();
            this.username = refund.getUser().getUsername();
            this.paymentId = refund.getPayment().getPaymentId(); // 주민번호
            this.amount = refund.getPayment().getAmount();
            this.reason = refund.getReason();
            this.statusDisplay = refund.getStatus().getDisplayName();
            if (refund.getCreatedAt() != null) {
                this.requestedAt = MyDateUtil.timestampFormat(refund.getCreatedAt());
            }
            this.pending = refund.getStatus() == RefundStatus.PENDING;
            this.approved = refund.getStatus() == RefundStatus.APPROVED;
        }
    }

    @Data
    public static class ApproveDto {
        private String paymentId; // 포트원 결제 건 식벌자 (경로로 활용 됨)
        private int amount; // DB 조회해야 알 수 있음

        public ApproveDto(String paymentId, int amount) {
            this.paymentId = paymentId;
            this.amount = amount;
        }
    }
}
