package com.dws.challenge.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.exception.AccountDoesNotExistsException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;

@Service
public class AccountsService {

	@Autowired
	private final AccountsRepository accountsRepository;

	@Autowired
	private final NotificationService notificationService;

	public AccountsRepository getAccountsRepository() {
		return accountsRepository;
	}

	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
	}

	@Transactional
	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		Account account = null;
		account = this.accountsRepository.getAccount(accountId);
		return account;
	}

	public boolean withdraw(Account account, BigDecimal amount) {
		if (account.lock.tryLock()) {
			try {
				account.setBalance(account.getBalance().subtract(amount));
				accountsRepository.updateBalance(account);
			} finally {
				account.lock.unlock();
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean deposit(Account account, BigDecimal amount) {
		if (getAccount(account.getAccountId()) == null)
			throw new AccountDoesNotExistsException();
		if (account.lock.tryLock()) {
			try {
				account.setBalance(account.getBalance().add(amount));
				accountsRepository.updateBalance(account);
			} finally {
				account.lock.unlock();
			}
			return true;
		} else {
			return false;
		}
	}

	@Transactional
	@Async
	public void transfetAmount(Transfer transfer) throws AccountDoesNotExistsException, InsufficientBalanceException {
		if (accountsRepository.getAccount(transfer.getToAccount()) == null) {
			throw new AccountDoesNotExistsException();
		}
		boolean success = false;
		while (!success) {
			BigDecimal balance = accountsRepository.getBalance(transfer.getFromAccount());
			if (balance.subtract(transfer.getAmount()).doubleValue() >= 0) {
				if (withdraw(getAccount(transfer.getFromAccount()), transfer.getAmount())) {
					if (deposit(getAccount(transfer.getToAccount()), transfer.getAmount())) {
						success = true;
					}
				} else {
					throw new InsufficientBalanceException();
				}
			}
		}
		this.notificationService.notifyAboutTransfer(getAccount(transfer.getFromAccount()),
				transfer.getAmount() + " amount transferred successfully to " + transfer.getToAccount());
		this.notificationService.notifyAboutTransfer(getAccount(transfer.getToAccount()),
				transfer.getAmount() + " received from" + transfer.getFromAccount());
	}
}
