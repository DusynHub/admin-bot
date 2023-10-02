package dev.vita.deletelinkbot.service;


import dev.vita.deletelinkbot.config.AdminBotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@PropertySource(value = "classpath:application.properties")
public class TelegramBot extends TelegramLongPollingBot {
  private final AdminBotConfig config;

  private final String logPrefix;

    @Autowired
    public TelegramBot(AdminBotConfig config,  @Value("${log.prefix}") String logPrefix) {
        this.config = config;
        this.logPrefix = logPrefix;
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Welcome message"));
        String commandsStr = commands.stream().map(curCom -> (curCom.getCommand() + " ")).toString();
        log.info(this.logPrefix + " Bot command: {}  were registered", commandsStr);
        try {
            this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(this.logPrefix + " Something went wrong while bot initialization");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();


    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info(logPrefix + " Update received");

        if (update.hasMessage() && update.getMessage().getNewChatMembers() != null) {
            update.getMessage().getNewChatMembers().forEach(user -> {
                System.out.println("!isUserSubscribed(user.getId()) = " + !isUserSubscribed(user.getId()));
                if (!isUserSubscribed(user.getId())) {
                    restrictUser(update.getMessage().getChatId(), user.getId());
                } else {
                    unrestrictUser(update.getMessage().getChatId(), user.getId());
                }
            });
        }
    }

    private boolean isUserSubscribed(Long userId) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId("@" + "alishev_g");
        getChatMember.setUserId(userId);
        try {
            ChatMember chatMember = execute(getChatMember);
            String status = chatMember.getStatus();
            System.out.println("status: " + status);
            return status.equals("member") || status.equals("creator") || status.equals("administrator");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return false;
        }
    }


    private void restrictUser(Long chatId, Long userId) {
        RestrictChatMember restrictUser = new RestrictChatMember();
        ChatPermissions chatPermissions = new ChatPermissions();
        chatPermissions.setCanSendMessages(false);
        restrictUser.setChatId(chatId);
        restrictUser.setUserId(userId);
        restrictUser.setPermissions(chatPermissions);
        try {
            execute(restrictUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void unrestrictUser(Long chatId, Long userId) {
        RestrictChatMember unrestrictUser = new RestrictChatMember();
        ChatPermissions chatPermissions = new ChatPermissions();
        chatPermissions.setCanSendOtherMessages(true);
        chatPermissions.setCanSendMediaMessages(true);
        chatPermissions.setCanSendMessages(true);
        unrestrictUser.setChatId(chatId);
        unrestrictUser.setUserId(userId);
        unrestrictUser.setPermissions(chatPermissions);

        try {
            execute(unrestrictUser);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }





    private void handleStart(Message message){
        User currentUser = message.getFrom();
        String startAnswer =
                String.format(  "Добрый день, %s это Delete link bot \n" +
                                "Этот бот при добавлении в чат удаляет все" +
                                " сообщения, которые содержат ссылки и отправлены не администраторами. " +
                                "Администраторы могут отправлять сообщения, содержащие ссылки. " +
                                " \n" +
                                "Для распознавания ссылок в тексте  используется библиотека \"URL Detector\" от linkedin.com \n" +
                                " \n" +
                                "--------------------------------[ВАЖНОЕ УСЛОВИЕ]--------------------------------\n" +
                                "Для работы бота необходимо сделать его администратором и дать разрешение на удаление сообщений"
                        , currentUser.getFirstName());
        try {
            this.execute(getSendMessage(message.getChatId(), message.getMessageId(), startAnswer));
        } catch (TelegramApiException e) {
            log.info(logPrefix + " Failed to answer to '/start' command");
            throw new RuntimeException(e);
        }
        log.info(logPrefix + " Answered to '/start' command");
    }

    private SendMessage getSendMessage(long chatId, int msgId, String startAnswer) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(msgId);
        sendMessage.setText(startAnswer);
        return sendMessage;
    }
}
