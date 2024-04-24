package com.dws.challenge.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.exception.AccountDoesNotExistsException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.AccountsService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {
	
	@Autowired
	private final AccountsService accountsService;

	public AccountsController(AccountsService accountsService) {
		this.accountsService = accountsService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
		// log.info("Creating account {}", account);

		try {
			this.accountsService.createAccount(account);
		} catch (DuplicateAccountIdException daie) {
			return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping(path = "/{accountId}")
	public Account getAccount(@PathVariable String accountId) {
		// log.info("Retrieving account for id {}", accountId);
		return this.accountsService.getAccount(accountId);
	}

	@PostMapping(path = "/transferAmount", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<Object> transfetAmount(@RequestBody Transfer transfer)
			throws AccountDoesNotExistsException {
		try {
			accountsService.transfetAmount(transfer);
		} catch (AccountDoesNotExistsException accountDoesNotExistsException) {
			throw new AccountDoesNotExistsException();
		} catch (InsufficientBalanceException insufficientBalanceException) {
			throw new InsufficientBalanceException();
		}
		return new  ResponseEntity<>(transfer.getAmount() + " transfered successfully to " + transfer.getToAccount(), HttpStatus.OK);
	}
	
	@PutMapping(path="/updateBalance", consumes= MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<Object> updateBalance(@RequestBody Account account) throws AccountDoesNotExistsException {
		Account origAccount = getAccount(account.getAccountId());
		accountsService.deposit(origAccount,account.getBalance());
		return new ResponseEntity<>("Balance updated successfully",HttpStatus.OK);
	}

	@ExceptionHandler(AccountDoesNotExistsException.class)
	public ResponseEntity<Object> handleAccountDoesNotExistsException() {
		return new ResponseEntity<>("Account Does not exists", HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public ResponseEntity<Object> handleInsufficientBalanceException() {
		return new ResponseEntity<>("Insufficinet balance", HttpStatus.FORBIDDEN);
	}

}
