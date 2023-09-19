import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrunchyBot extends TelegramLongPollingBot {
    private MessageSender messageSender;
    private Message message;
    private String helpMessage = "https://docs.google.com/spreadsheets/d/1S-6LVVqPcS52mJs8QYna_NfUtwW7lV-JK_O5ZExBkmQ/edit#gid=0";

    private String dataFile = "userdata.txt";
    private final File file = new File("userdata.txt");

    @Override
    public void onUpdateReceived(Update update) {
        message = update.getMessage();
        if (update.hasCallbackQuery()) {
            handleButton(update);
        } else {
            handleMessage(update);
        }
    }
    private void sendDailySchedule(TelegramLongPollingBot bot) {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) ;
        String dayOfWeekString = getDayOfWeekString(dayOfWeek);
        // Чтение информации из файла userdata.txt
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    Long chat = Long.parseLong(parts[0]);
                    String groupNum = parts[1];
                    System.out.println(dayOfWeekString + "," + chat + "," + groupNum);
                    if (!dayOfWeekString.equals("недействительный день недели")) {
                        try {
                            GoogleSheetsScraper gss = new GoogleSheetsScraper();
                            String result = gss.sheetsParser(groupNum, dayOfWeekString);
                            SendMessage sm = SendMessage.builder()
                                    .chatId(chat.toString())
                                    .text(result).build();
                            bot.execute(sm);
                        } catch (IOException | TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void handleButton(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Message messageHelp = callbackQuery.getMessage();
        messageHelp.setText("/help");
        try {
            messageSender = new MessageSender(messageHelp, this, update);
            System.out.println(messageSender.getMessage().getText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Thread(messageSender).start();
    }

    private void handleMessage(Update update) {
        MessageSender messageSender;
        try {
            messageSender = new MessageSender(message, this, update);
            new Thread(messageSender).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        CrunchyBot bot = new CrunchyBot();
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        LocalTime currentTime = LocalTime.now();
        LocalTime desiredTime = LocalTime.of(7,30);
        long initialDelay = Duration.between(currentTime, desiredTime).toMillis();
        if (initialDelay < 0) {
            initialDelay += 24 * 60 * 60 * 1000;
        }
        scheduler.scheduleAtFixedRate(() -> {

            bot.sendDailySchedule(bot);
        }, initialDelay, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getBotToken() {
        return "xxxxxxxxxxxxxxxxx";
    }

    @Override
    public String getBotUsername() {
        return "itis_timetablebot";
    }

    public static String getDayOfWeekString(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "воскресенье";
            case Calendar.MONDAY:
                return "понедельник";
            case Calendar.TUESDAY:
                return "вторник";
            case Calendar.WEDNESDAY:
                return "среда";
            case Calendar.THURSDAY:
                return "четверг";
            case Calendar.FRIDAY:
                return "пятница";
            case Calendar.SATURDAY:
                return "суббота";
            default:
                return "недействительный день недели";
        }
    }

}
//pgrep java | killall -9 java
//nohup java -jar "t_bot5".jar -b > log.txt 2>1&
