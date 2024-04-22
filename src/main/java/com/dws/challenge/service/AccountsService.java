package com.dws.challenge.service;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
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

	private final ReentrantLock reentrantLock;

	public AccountsRepository getAccountsRepository() {
		return accountsRepository;
	}

	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
		this.reentrantLock = new ReentrantLock(true);
	}

	public ReentrantLock getReentrantLock() {
		return reentrantLock;
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
		if (reentrantLock.tryLock()) {
			try {
				BigDecimal bal = account.getBalance();
				accountsRepository.updateBalance(account.getAccountId(), bal.subtract(amount));
			} finally {
				reentrantLock.unlock();
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean deposit(Account account, BigDecimal amount) {
		if(getAccount(account.getAccountId())== null)
			throw  new AccountDoesNotExistsException();
		if (reentrantLock.tryLock()) {
			try {
				BigDecimal bal = account.getBalance();
				accountsRepository.updateBalance(account.getAccountId(), bal.add(amount));
			} finally {
				reentrantLock.unlock();
			}
			return true;
		} else {
			return false;
		}
	}

	public void transfetAmount(Transfer transfer) throws AccountDoesNotExistsException, InsufficientBalanceException {
		if (accountsRepository.getAccount(transfer.getToAccount()) == null) {
			throw new AccountDoesNotExistsException();
		}
		boolean success = false;
		while (!success) {
			if (reentrantLock.tryLock()) {
				try {
					BigDecimal balance = accountsRepository.getBalance(transfer.getFromAccount());
					if (balance.subtract(transfer.getAmount()).doubleValue() >= 0) {
						if (withdraw(getAccount(transfer.getFromAccount()), transfer.getAmount())) {
							if (deposit(getAccount(transfer.getToAccount()), transfer.getAmount())) {
								success = true;
							}
						}
					} else {
						throw new InsufficientBalanceException();
					}
				} finally {
					reentrantLock.unlock();
				}
			}
		}
		if (success) {
			this.notificationService.notifyAboutTransfer(getAccount(transfer.getFromAccount()),
					transfer.getAmount() + " amount transferred successfully to " + transfer.getToAccount());
			this.notificationService.notifyAboutTransfer(getAccount(transfer.getToAccount()),
					transfer.getAmount() + " received from" + transfer.getFromAccount());
		}
	}
}
