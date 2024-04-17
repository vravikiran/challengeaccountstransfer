package com.dws.challenge.service;

import org.springframework.stereotype.Service;

import com.dws.challenge.domain.Account;
@Service
public interface NotificationService {

  void notifyAboutTransfer(Account account, String transferDescription);
}
