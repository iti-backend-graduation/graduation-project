# ATM and POS Troubleshooting Guide

**Document ID:** BM-CRD-004 | **Version:** 1.0 | **Effective:** 2026-01-01 | **Owner:** ATM Operations

*SYNTHETIC TRAINING DOCUMENT - for educational simulation only.*

## 1. Purpose and Scope

This guide helps branch employees deal with common customer problems at ATMs and POS terminals. It explains what the employee can check and fix directly, what must be escalated and to whom, and what to tell the customer. It covers cash that was not dispensed, cards retained by an ATM, receipt problems, POS declines, decline reason codes and transaction limits. Lost and stolen cards are covered in BM-CRD-002, disputes in BM-CRD-003 and general card rules in BM-CRD-001.

## 2. ATM Problems

### 2.1 Cash Not Dispensed or Partly Dispensed

When the customer says the ATM debited the account but did not give the cash, or gave less than requested, the employee must first confirm the identity of the customer and ask for the date, time, amount and location of the ATM, and for the receipt if the customer has one. The employee then checks the account for the debit. If the debit is there, the employee registers a cash-not-dispensed claim under the CARD_SERVICE category and sends it to ATM Operations. ATM Operations reconciles the ATM cash counters and the journal. If the ATM was over in cash, the amount is returned to the account. The refund is normally made within 5 working days for ATMs of the bank. For another bank's ATM the refund can take longer, and the employee should tell the customer that the other bank must confirm first. If the amount is not returned and the customer wants to dispute the debit, the employee follows BM-CRD-003 and the customer must file within 60 days of the statement date.

The employee must not tell the customer that the money will definitely be returned before ATM Operations confirms the result. If the debit is not found on the account, the employee tells the customer that no money was taken, and may check again after 1 working day because some pending debits post late.

### 2.2 Card Retained by the ATM

An ATM keeps a card for one of these reasons: the card was not taken within 30 seconds, the PIN was wrong 3 times, the card is blocked or expired, or the ATM had a hardware fault. The employee verifies the customer's identity and checks the ATM retention log in the card system. If the card was retained at a branch of the bank, ATM Operations collects it on the next visit, normally within 2 working days. The employee should not promise the customer that the card will be given back. A card retained after 3 wrong PIN attempts is blocked, and the customer can have it unblocked in the branch with the original ID. A card retained because it was blocked or expired is never returned and must be replaced under BM-CRD-002. If the card cannot be found within 2 working days, the employee treats it as lost and the supervisor approves the replacement.

### 2.3 Receipt Issues

If the ATM did not print a receipt, the employee tells the customer that the receipt is not needed for a valid transaction and that the customer can check the transaction in BM Digital or in the account history at the branch. If the customer needs proof of a past ATM transaction, the employee may print a duplicate of the account statement. A duplicate statement copy costs EGP 30 per statement. The employee may not issue a fake or handwritten receipt for an ATM transaction.

### 2.4 Balance Inquiry and Fees at Other Banks

An ATM withdrawal at another bank's ATM costs EGP 8 and a balance inquiry at another bank's ATM costs EGP 3. These fees are not refunded when the transaction fails because of a customer error. If the withdrawal failed because of a technical fault of the other bank, the employee may open a request, and the fee can be refunded after ATM Operations confirms the fault.

## 3. POS Problems

### 3.1 Declined Purchases

When a purchase is declined, the terminal prints or shows a decline reason code. The employee asks the customer for the code or the terminal slip, then looks up the code in the table below. The codes below are the codes used in this guide.

| Code | Meaning | What the employee can do |
|------|---------|--------------------------|
| D01 | Insufficient funds or available credit | Explain the balance; the customer may deposit money or pay a credit card bill |
| D02 | Daily POS limit exceeded | Explain the limit; the customer can ask for a limit increase |
| D03 | Wrong PIN entered | Ask the customer to retry; after 3 wrong attempts the card is blocked |
| D04 | Card blocked or expired | Check the status; unblock if ID is verified, or request a renewal |
| D05 | Online or international use disabled | Ask the customer to enable the control in BM Digital |
| D06 | Suspected fraud hold | Escalate to the supervisor; do not release the hold |

### 3.2 Handling Each Code

For D01 the employee checks the available balance and tells the customer the exact amount that is available, after confirming identity. For D02 the employee explains the daily POS limit of the card. The limit is EGP 50,000 for Classic Debit and EGP 100,000 for Platinum Debit, and it resets at midnight. For D03 the employee reminds the customer that 3 wrong PIN attempts block the card. For D04 the employee checks whether the card is blocked for PIN attempts, lost, or expired, and follows BM-CRD-001 or BM-CRD-002 accordingly. For D05 the customer can switch on online or international use in BM Digital, and the change is effective immediately. For D06 the employee must not remove the hold. The employee only tells the customer that the card was stopped for security and that the Fraud Monitoring team will contact the customer through the registered mobile number.

### 3.3 Double Charge at a POS

If a customer was charged twice at the same terminal, the employee asks for both slips or the statement lines and registers a dispute under BM-CRD-003. One of the two debits is often a pending authorisation that disappears by itself within 5 working days, so the employee should tell the customer to wait before filing, unless both debits already appear as posted.

## 4. Limits

The daily ATM limit is EGP 15,000 for Classic Debit and EGP 30,000 for Platinum Debit. The daily POS limit is EGP 50,000 for Classic Debit and EGP 100,000 for Platinum Debit. A customer who needs a higher limit than the card allows cannot be given an exception at the counter. A limit change within the maximum of the card needs supervisor approval and takes effect within 1 working day. Credit card purchases are limited by the available credit limit, and an over-limit transaction can cause a fee of EGP 100.

## 5. What the Employee Can Do and What Must Be Escalated

The employee can verify identity, check balances and account history, explain decline codes and limits, register cash-not-dispensed claims, print duplicate statements, guide the customer to change a PIN or enable controls in BM Digital, unblock a card after ID verification and raise replacement requests for approval.

The employee must escalate to the supervisor any fraud hold (D06), any fee waiver, any limit increase, and any card replacement. The supervisor may waive up to EGP 200 per request. The employee must escalate to ATM Operations any suspected ATM fault, any cash-not-dispensed claim and any retained card that cannot be found. The employee must never ask for the PIN or the OTP, never open an ATM cabinet or handle ATM cash, and never promise a refund before ATM Operations confirms it.
