package com.dws.challenge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountDoesNotExistsException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.AccountsService;

@SpringBootTest
@AutoConfigureMockMvc
class AccountsControllerTest {
	@Autowired(required = true)
	private MockMvc mockMvc;
	@MockBean
	private AccountsService accountsService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	void prepareMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

	}

	@Test
	void createAccount() throws Exception {
		doNothing().when(accountsService).createAccount(any());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());
		verify(accountsService,times(1)).createAccount(any());
	}

	@Test
	void createDuplicateAccount() throws Exception {
		doThrow(DuplicateAccountIdException.class).when(accountsService).createAccount(any());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"balance\":1000}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoBalance() throws Exception {
		this.mockMvc.perform(
				post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":\"Id-123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoBody() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNegativeBalance() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void createAccountEmptyAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void transferAmountSuccess() throws Exception {
		doNothing().when(accountsService).transfetAmount(any());
		mockMvc.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccount\":\"1234\", \"toAccount\":\"1235\", \"amount\":1000}"))
				.andExpect(status().isOk());
	}
	
	@Test
	void transferAmountFail_InValidToAccount() throws Exception {
		doThrow(AccountDoesNotExistsException.class).when(accountsService).transfetAmount(any());
		mockMvc.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccount\":\"1234\", \"toAccount\":\"1235\", \"amount\":1000}"))
				.andExpect(status().is4xxClientError());
	}
	
	@Test
	void transferAmountFail_InSuffAmount() throws Exception {
		doThrow(InsufficientBalanceException.class).when(accountsService).transfetAmount(any());
		mockMvc.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fromAccount\":\"1234\", \"toAccount\":\"1235\", \"amount\":1000}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void getAccount() throws Exception {
		String uniqueAccountId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
		this.accountsService.createAccount(account);
		when(accountsService.getAccount(anyString())).thenReturn(account);
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
	}
}
