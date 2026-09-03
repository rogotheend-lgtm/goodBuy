ALTER TABLE expense_transaction
    RENAME COLUMN requires_review TO anomaly;

ALTER TABLE expense_transaction
    RENAME COLUMN review_reason TO anomaly_reason;

ALTER TABLE expense_transaction
    ADD COLUMN anomaly_detail VARCHAR(300);

UPDATE expense_transaction
SET transaction_type = 'ANOMALY'
WHERE transaction_type = 'NEEDS_REVIEW';

UPDATE expense_transaction
SET anomaly = TRUE,
    anomaly_reason = 'SELF_TRANSFER',
    anomaly_detail = '입력한 이름과 거래 상대명이 일치하여 본인 계좌 이체로 판단했습니다. 소비 합계에서 제외했습니다.'
WHERE transaction_type = 'SELF_TRANSFER';

UPDATE expense_transaction
SET anomaly_detail = CASE anomaly_reason
    WHEN 'AMBIGUOUS_PAYMENT_GATEWAY'
        THEN '결제 플랫폼명만으로 결제와 송금을 구분할 수 없어 이상치로 표시했습니다. 소비 합계에서 제외했습니다.'
    WHEN 'GROUP_PAYMENT_CANDIDATE'
        THEN '업종별 기준 금액보다 큰 거래입니다. 단체 결제 가능성이 있어 소비 합계에서 제외했습니다.'
    ELSE anomaly_detail
END
WHERE anomaly = TRUE
  AND anomaly_detail IS NULL;
