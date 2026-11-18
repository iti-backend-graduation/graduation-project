# Digital Banking Security and Authentication Policy

**Document ID:** BM-DIG-002 | **Version:** 1.0 | **Effective:** 2026-01-01 | **Owner:** Information Security

*SYNTHETIC TRAINING DOCUMENT - for educational simulation only.*

## 1. Purpose and Scope

This policy defines the authentication and security rules for BM Digital and online banking, and the actions that branch employees and supervisors must take when security events occur. It covers OTP rules, passwords, profile lockout and unlock, device binding, biometric login, phishing awareness, change of registered mobile number and handling of suspicious activity. It applies to all branch employees who support retail digital banking customers.

## 2. One-Time Password Rules

### 2.1 OTP Delivery and Validity

The OTP is sent by SMS to the mobile number registered with the bank. Each OTP is valid for 3 minutes and can be used only once. After 3 minutes the OTP expires and the customer must request a new one. Requesting a new OTP cancels the previous one, so the customer must enter the latest code received.

### 2.2 OTP Attempts and Resend

The customer may request a new OTP up to 5 times within 30 minutes. After that, OTP requests are paused for 30 minutes. An OTP entered wrongly 3 times in a row cancels that code and a new one must be requested.

### 2.3 Employee Obligations

No employee, supervisor or call center agent may ask a customer to read out an OTP, whether in person, by phone or by message. The employee must not view the SMS inbox of the customer's phone. If a customer shares an OTP with an employee by mistake, the employee must tell the customer that the code is now unsafe and ask the customer to request a new one.

If the OTP does not arrive, the employee first checks that the registered mobile number is correct, that the phone has signal and storage space, and that the customer has not blocked messages from short-code senders. If the registered number is wrong or the customer lost the SIM, the mobile number change procedure in section 7 applies.

## 3. Passwords

### 3.1 Password Rules

A BM Digital password must have at least 10 characters and include at least one letter, one digit and one symbol. The password must not contain the username or the national ID number. The password expires every 180 days. The app warns the customer 14 days before expiry. The new password cannot match any of the last 3 passwords used.

### 3.2 Forgotten or Expired Password

The customer selects "Forgot password" on the login screen, enters the national ID number and debit card number, and confirms with an OTP. The customer then creates a new password. This self-service reset does not need an employee. If the customer cannot complete the reset because the profile is locked, the lockout procedure in section 4 applies.

### 3.3 Password Safety

The employee must remind the customer never to write the password on the card, never to share it with family members and not to reuse a password used on other websites.

## 4. Profile Lockout and Unlock

### 4.1 When a Profile Locks

5 consecutive failed login attempts lock the profile for 24 hours. The counter resets after a successful login. A locked profile cannot be used on the app or on the website, but the customer's cards and accounts continue to work normally at ATMs, POS terminals and branches.

### 4.2 Waiting Period

If the customer is not in a hurry, the profile unlocks automatically after 24 hours and the customer can log in with the correct password or use "Forgot password". The employee must explain this option first.

### 4.3 Supervisor Unlock Procedure

If the customer needs access before the 24 hours end, a supervisor can unlock the profile after in-branch ID verification. The steps are as follows.

1. The customer visits the branch with the original valid national ID. A copy or a photo of the ID is not accepted. A non-resident customer shows the valid passport.
2. The employee compares the photo and details on the ID with the customer and with the profile record.
3. The employee raises the unlock request in the system and asks the supervisor to review.
4. The supervisor verifies the customer personally and approves the request. An employee cannot approve an unlock request. The supervisor cannot approve a request for a customer who is not present in the branch.
5. After approval the profile is unlocked and the customer is asked to reset the password immediately.

Unlock requests cannot be made over the phone or by message. If an unlock is requested more than 2 times in 30 days for the same customer, the supervisor must record the reason and notify Information Security because repeated locks can indicate attempted fraud.

## 5. Device Binding and Biometric Login

### 5.1 Device Binding

A customer can bind a phone to the profile after the first OTP login. A maximum of 2 devices can be bound to one profile at any time. Binding a third device is rejected until one of the existing devices is removed. The customer can remove a bound device from the app settings on the other device, or by visiting the branch.

A new binding on a phone creates a push notification and an SMS to the customer. Any binding that the customer did not make must be reported as suspicious activity under section 8.

### 5.2 Changing the Phone

When the customer changes the phone, the customer installs BM Digital on the new phone, logs in with the password and OTP, and binds the new phone. If this would be the third device, the customer removes an older device first. A customer who has lost the old phone and cannot log in must come to the branch with ID so that the old device can be removed after verification by a supervisor.

### 5.3 Biometric Login

Biometric login with fingerprint or face recognition is optional and available only on a bound device. The biometric data stays in the phone and is never sent to the bank. Biometric login is used for log in and for confirming transfers up to EGP 10,000. Transfers above EGP 10,000 and any new beneficiary require the password and an OTP. If the customer changes the biometric settings of the phone, for example by adding a new fingerprint, the app disables biometric login until the customer enters the password again.

## 6. Phishing and Fraud Awareness

The bank never asks customers for passwords, PINs or OTPs by phone, SMS, email or social media. The employee must teach customers these rules.

- Do not click links in messages that claim to be from the bank. Open the app directly.
- The bank will never ask the customer to move money to a "safe account".
- Do not install apps that allow remote control of the phone, even if a caller claims to be from the bank.
- Check the sender and the address of any website before entering a username.
- Report suspicious messages to the Contact Center hotline 16555, which is available 24/7.

Branch employees must not send customers links by messaging applications. They must refer customers to the app store or to the official website address displayed in the branch.

## 7. Change of Registered Mobile Number

The registered mobile number is the key to OTP delivery, so changing it is a high-risk action. The procedure is as follows.

1. The customer visits the branch in person with the original national ID. Requests cannot be accepted over the phone, by email or through an agent.
2. The employee verifies the ID and the signature against the signature specimen card.
3. The customer completes the change request form with the new mobile number, which must be in the name of the customer or confirmed by the customer as personally used.
4. The employee raises the request and the supervisor reviews it and approves it. Without supervisor approval the change cannot be executed.
5. The change takes effect within 1 working day after approval. The bank sends an SMS to both the old number and the new number when the change is completed.

While the change is pending, the customer should not expect OTP messages at the new number. Existing bound devices stay bound, but the supervisor may ask Information Security to remove them if the customer reports that the phone was lost or stolen. When the customer reports a stolen phone, the employee also advises the customer to block the SIM with the mobile operator.

## 8. Suspicious Activity Handling

### 8.1 What Counts as Suspicious

Suspicious activity includes login notifications that the customer did not trigger, device binding or password change that the customer did not make, OTPs that arrive without any request, transfers the customer does not recognise, and the customer receiving a call from someone claiming to be a bank officer.

### 8.2 Employee Actions

1. Stay calm and verify the identity of the customer with the original national ID.
2. Advise the customer to change the password immediately using "Forgot password" and remove unknown bound devices.
3. If money has left the account, ask the customer to call the Contact Center hotline 16555 at once and raise a DIGITAL_BANKING service request marked urgent.
4. Record the time, the amount, the beneficiary details and the reference numbers of any transaction in question.
5. Inform the supervisor, who notifies Information Security on the same working day.

The employee must not promise a refund or decide who is liable. Liability for unauthorised digital transactions is decided by Information Security after investigation. The employee must tell the customer only that the case will be reviewed and give the service request reference number.
