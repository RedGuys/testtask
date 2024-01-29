package ru.redguy.testtask;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;

import static ru.redguy.testtask.Task3.ContractState.*;

public class Task3 {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new Bot(System.getenv("TOKEN")));
    }

    public static class TwoKeyMap<K, V> extends HashMap<K, V> {
        //allow to get value by one of two keys
        public void put(K key1, K key2, V value) {
            super.put(key1, value);
            super.put(key2, value);
        }
    }

    public static class Contract {
        public ContractState state = WAITING_FOR_SECOND_USER;
        public long initiatorUserId;
        public long secondUserId;

        public Contract(long initiatorUserId, long secondUserId) {
            this.initiatorUserId = initiatorUserId;
            this.secondUserId = secondUserId;
        }
    }

    public enum ContractState {
        WAITING_FOR_SECOND_USER, //ожидание подтверждения начала контракта от второго пользователя
        WAITING_FOR_INITIATOR_ACTION, //ожидание действия от инициатора
        WAITING_FOR_SECOND_USER_APPROVE, //ожидание подтверждения от второго пользователя
        WAITING_FOR_SECOND_USER_ACTION, //ожидание действия от второго пользователя
        WAITING_FOR_INITIATOR_APPROVE, //ожидание подтверждения от инициатора
        DONE, //контракт завершен
        CANCELED, //контракт отменен
        WAITING_FOR_SECOND_USER_CANCEL, //ожидание отмены от второго пользователя
        WAITING_FOR_INITIATOR_CANCEL_APPROVE //ожидание подтверждения отмены от инициатора
    }

    public static class Bot extends TelegramLongPollingBot {

        TwoKeyMap<Long, Contract> contracts = new TwoKeyMap<>();

        public Bot(String botToken) {
            super(botToken);
        }

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                String[] parts = text.split(" ");
                switch (parts[0]) {
                    case "/start" -> {
                        reply(update, "Привет, для начала контракта отправь /contract (user id)");
                    }
                    case "/contract" -> {
                        if (parts.length == 2) {
                            if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                                reply(update, "У вас уже есть активный контракт");
                                break;
                            }
                            if (contracts.containsKey(Long.parseLong(parts[1]))) {
                                reply(update, "У пользователя уже есть активный контракт");
                                break;
                            }
                            contracts.put(update.getMessage().getFrom().getId(), Long.parseLong(parts[1]), new Contract(update.getMessage().getFrom().getId(), Long.parseLong(parts[1])));
                            reply(update, "Ожидание подтверждения от пользователя, для отмены отправьте /cancel");
                            sendMessage(Long.parseLong(parts[1]), "Пользователь " + update.getMessage().getFrom().getId() + " предлагает вам контракт, для подтверждения отправьте /accept, для отмены отправьте /cancel");
                        } else {
                            reply(update, "Неверное количество аргументов");
                        }
                    }
                    case "/accept" -> {
                        if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                            Contract contract = contracts.get(update.getMessage().getFrom().getId());
                            if(contract.secondUserId != update.getMessage().getFrom().getId()) {
                                reply(update, "Вы не являетесь вторым пользователем в контракте");
                                break;
                            }
                            if (contract.state == ContractState.WAITING_FOR_SECOND_USER) {
                                contract.state = ContractState.WAITING_FOR_INITIATOR_ACTION;
                                reply(update, "Контракт подтвержден, ожидание действия от инициатора");
                                sendMessage(contract.initiatorUserId, "Контракт подтвержден, выполните действия со своей стороны, когда закончите отправьте /done");
                            } else {
                                reply(update, "Контракт уже подтвержден");
                            }
                        } else {
                            reply(update, "У вас нет активного контракта");
                        }
                    }
                    case "/done" -> {
                        if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                            Contract contract = contracts.get(update.getMessage().getFrom().getId());
                            if (contract.initiatorUserId != update.getMessage().getFrom().getId()) {
                                reply(update, "Вы не являетесь инициатором в контракте");
                                break;
                            }
                            if (contract.state == ContractState.WAITING_FOR_INITIATOR_ACTION) {
                                contract.state = ContractState.WAITING_FOR_SECOND_USER_APPROVE;
                                reply(update, "Действие подтверждено, ожидание подтверждения от второго пользователя");
                                sendMessage(contract.secondUserId, "Пользователь " + update.getMessage().getFrom().getId() + " выполнил действие, для подтверждения отправьте /approve");
                            } else {
                                reply(update, "Действие уже подтверждено");
                            }
                        } else {
                            reply(update, "У вас нет активного контракта");
                        }
                    }
                    case "/approve" -> {
                        if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                            Contract contract = contracts.get(update.getMessage().getFrom().getId());
                            if (contract.secondUserId != update.getMessage().getFrom().getId()) {
                                reply(update, "Вы не являетесь вторым пользователем в контракте");
                                break;
                            }
                            if (contract.state == ContractState.WAITING_FOR_SECOND_USER_APPROVE) {
                                contract.state = ContractState.WAITING_FOR_SECOND_USER_ACTION;
                                reply(update, "Действие подтверждено, ожидание ваших действий, когда закончите отправьте /ready");
                                sendMessage(contract.initiatorUserId, "Пользователь " + update.getMessage().getFrom().getId() + " подтвердил действие, ожидание действий от второй стороны");
                            } else {
                                reply(update, "Действие уже подтверждено");
                            }
                        } else {
                            reply(update, "У вас нет активного контракта");
                        }
                    }
                    case "/ready" -> {
                        if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                            Contract contract = contracts.get(update.getMessage().getFrom().getId());
                            if (contract.secondUserId != update.getMessage().getFrom().getId()) {
                                reply(update, "Вы не являетесь пользователем в контракте");
                                break;
                            }
                            if (contract.state == ContractState.WAITING_FOR_SECOND_USER_ACTION) {
                                contract.state = ContractState.WAITING_FOR_INITIATOR_APPROVE;
                                reply(update, "Действие подтверждено, ожидание подтверждения от инициатора");
                                sendMessage(contract.initiatorUserId, "Пользователь " + update.getMessage().getFrom().getId() + " выполнил действие, для подтверждения отправьте /approved");
                            } else {
                                reply(update, "Действие уже подтверждено");
                            }
                        } else {
                            reply(update, "У вас нет активного контракта");
                        }
                    }
                    case "/approved" -> {
                        if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                            Contract contract = contracts.get(update.getMessage().getFrom().getId());
                            if (contract.initiatorUserId != update.getMessage().getFrom().getId()) {
                                reply(update, "Вы не являетесь инициатором в контракте");
                                break;
                            }
                            if (contract.state == ContractState.WAITING_FOR_INITIATOR_APPROVE) {
                                contract.state = ContractState.DONE;
                                reply(update, "Действие подтверждено, контракт завершен");
                                sendMessage(contract.secondUserId, "Инициатор " + update.getMessage().getFrom().getId() + " подтвердил действие, контракт завершен");
                                contracts.remove(contract.initiatorUserId);
                                contracts.remove(contract.secondUserId);
                            } else {
                                reply(update, "Действие уже подтверждено");
                            }
                        } else {
                            reply(update, "У вас нет активного контракта");
                        }
                    }
                    case "/cancel" -> {
                        if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                            Contract contract = contracts.get(update.getMessage().getFrom().getId());
                            if (contract.state == WAITING_FOR_SECOND_USER || contract.state == WAITING_FOR_INITIATOR_ACTION) {
                                contract.state = ContractState.CANCELED;
                                reply(update, "Контракт отменен");
                                sendMessage(contract.initiatorUserId, "Пользователь " + update.getMessage().getFrom().getId() + " отменил контракт");
                                contracts.remove(contract.initiatorUserId);
                                contracts.remove(contract.secondUserId);
                            } else if(contract.state == CANCELED || contract.state == DONE) {
                                reply(update, "Контракт уже отменен");
                            } else {
                                contract.state = ContractState.WAITING_FOR_SECOND_USER_CANCEL;
                                reply(update, "Контракт отменен, ожидание подтверждения от второго пользователя");
                                sendMessage(contract.secondUserId, "Пользователь " + update.getMessage().getFrom().getId() + " отменил контракт, для подтверждения отправьте /cancel_approve");
                            }
                        } else {
                            reply(update, "У вас нет активного контракта");
                        }
                    }
                    case "/cancel_approve" -> {
                        if (contracts.containsKey(update.getMessage().getFrom().getId())) {
                            Contract contract = contracts.get(update.getMessage().getFrom().getId());
                            if (contract.state == ContractState.WAITING_FOR_SECOND_USER_CANCEL) {
                                contract.state = ContractState.CANCELED;
                                reply(update, "Контракт отменен");
                                sendMessage(contract.initiatorUserId, "Пользователь " + update.getMessage().getFrom().getId() + " отменил контракт");
                                contracts.remove(contract.initiatorUserId);
                                contracts.remove(contract.secondUserId);
                            } else {
                                reply(update, "Контракт уже отменен");
                            }
                        } else {
                            reply(update, "У вас нет активного контракта");
                        }
                    }
                }
            }
        }

        public void reply(Update update, String text) {
            try {
                execute(new SendMessage(update.getMessage().getChat().getId().toString(), text));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(long userId, String text) {
            try {
                execute(new SendMessage(String.valueOf(userId), text));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String getBotUsername() {
            return "Somertestbot";
        }

        @Override
        public String getBotToken() {
            return super.getBotToken();
        }
    }
}
