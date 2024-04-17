package com.dws.challenge.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.exception.AccountDoesNotExistsException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;

@Service
@Transactional
public class AccountsService {

	@Autowired
	private final AccountsRepository accountsRepository;

	@Autowired
	private NotificationService notificationService;

	public AccountsRepository getAccountsRepository() {
		return accountsRepository;
	}

	public AccountsService(AccountsRepository accountsRepository,NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
	}

	public NotificationService getNotificationService() {
		return notificationService;
	}

	public void setNotificationService(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		Account account = this.accountsRepository.getAccount(accountId);
		return account;
	}

	public Account updateBalance(Account account) throws AccountDoesNotExistsException {
		Account origAcc = accountsRepository.getAccount(account.getAccountId());
		if (origAcc != null) {
			return this.accountsRepository.updateBalance(account.getAccountId(),
					origAcc.getBalance().add(account.getBalance()));
		} else {
			throw new AccountDoesNotExistsException();
		}
	}

	public void transfetAmount(Transfer transfer) throws AccountDoesNotExistsException, InsufficientBalanceException {
		if (accountsRepository.getAccount(transfer.getToAccount()) == null) {
			throw new AccountDoesNotExistsException();
		}
		BigDecimal balance = accountsRepository.getBalance(transfer.getFromAccount());
		if (balance.subtract(transfer.getAmount()).doubleValue() > 0) {
			accountsRepository.updateBalance(transfer.getToAccount(), transfer.getAmount());
			accountsRepository.updateBalance(transfer.getFromAccount(), balance.subtract(transfer.getAmount()));
			this.notificationService.notifyAboutTransfer(getAccount(transfer.getFromAccount()),
					transfer.getAmount() + " amount transferred successfully to " + transfer.getToAccount());
			this.notificationService.notifyAboutTransfer(getAccount(transfer.getToAccount()),
					transfer.getAmount() + " received from" + transfer.getFromAccount());
		} else {
			throw new InsufficientBalanceException();
		}
	}
}
