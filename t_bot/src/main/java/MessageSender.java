import lombok.Getter;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class MessageSender implements Runnable {

    private Message message;
    private Long who;
    private TelegramLongPollingBot bot;
    private String helpMessage = "https://docs.google.com/spreadsheets/d/1S-6LVVqPcS52mJs8QYna_NfUtwW7lV-JK_O5ZExBkmQ/edit#gid=0";
    private String startMessage = "Привет, я бот помогающий разобраться с расписанием, введи сообщение в формате \"номер группы,день недели\".Если хочешь получать рассылку расписания каждый день, напиши номер группы.Чтобы получить доступ к расписанию в виде гугл таблицы, набери /help";
    private GoogleSheetsScraper gss;
    private Update update;
    private String[] days = {"понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "пн", "вт", "ср", "чт", "пт", "сб"};
    private String dataFile = "userdata.txt";
    private final Map<Long, String> userResponses = new HashMap<>();


    public MessageSender(Message message, TelegramLongPollingBot bot, Update update) throws IOException {
        this.who = message.getChatId();
        this.bot = bot;
        this.message = message;
        this.update = update;
        this.gss = new GoogleSheetsScraper();
    }

    @Override
    public void run() {
        if (message.isCommand()) {
            handleCommand();
        } else {
            handleMessage();
        }
        System.out.println(Thread.currentThread().getId() + "," +
                message.getText() + ", " +
                message.getFrom().getUserName());

    }

    private void handleMessage() {
        String text = message.getText();
        if (text.equals("/help")) {
            sendText(who, helpMessage, bot);
        } else if (text.matches(".*,.*")) {
            String[] splitedMessage = message.getText().split(",");
            String firstWord = splitedMessage[0].replace(" ", "");
            String secondWord = splitedMessage[1].toLowerCase().replace(" ", "");
            if ((firstWord.matches("\\d{2}-\\d{3}") || firstWord.matches("^\\d{3}$")) && dayChecker(secondWord)) {
                try {
                    String result = gss.sheetsParser(firstWord, secondWord);
                    sendText(who, result, bot);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                sendText(who, "Ты ввел не в соответствии с форматом, посмотри внимательнее", bot);
            }
        } else if (text.matches("\\d{2}-\\d{3}")) {
            handleUserResponse(update, text);
        } else {
            sendText(who, "Я не знаю как расшифровать твое сообщение", bot);
        }
    }

    private void handleCommand() {
        switch (message.getText()) {
            case "/start":
                sendTextWithButton(who, startMessage, bot);
                break;
            case "/help":
                sendText(who, helpMessage, bot);
                break;
            default:
                sendText(who, "Неизвестная команда", bot);
                break;
        }
    }

    public void sendTextWithButton(Long who, String what, TelegramLongPollingBot bot) {
        SendMessage response = new SendMessage().builder()
                .chatId(who.toString())
                .text(what).build();
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton helpButton = new InlineKeyboardButton();
        helpButton.setText("Получить ссылку");
        helpButton.setCallbackData("/help");
        row.add(helpButton);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        response.setReplyMarkup(keyboardMarkup);
        try {
            bot.execute(response);
        } catch (TelegramApiException e) {
        }
    }

    public void sendText(Long who, String what, TelegramLongPollingBot bot) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            bot.execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean dayChecker(String day) {
        boolean dayFlag = false;
        for (int i = 0; i < days.length; i++) {
            if (days[i].equals(day)) {
                dayFlag = true;
                break;
            } else {
                dayFlag = false;
            }
        }
        return dayFlag;
    }

    private void saveUserData(String userResponse, Long chatId) {
        try (FileWriter writer = new FileWriter(dataFile, true);
             BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String line;
            boolean chatIdExists = false;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    Long existingChatId = Long.parseLong(parts[0]);
                    if (existingChatId.equals(chatId)) {
                        chatIdExists = true;
                        break;
                    }
                }
            }
            if (!chatIdExists) {
                writer.write( "\n" + chatId + ":" + userResponse);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void handleUserResponse (Update update, String userResponse) {
        Long chatId = update.getMessage().getChatId();
        saveUserData(userResponse, chatId);
        sendText(chatId, "Я успешно записал тебя в свой блокнотик. Теперь ты будешь получать расписание каждый день.", bot);
    }
}
