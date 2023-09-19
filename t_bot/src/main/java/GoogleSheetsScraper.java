import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Getter
public class GoogleSheetsScraper {
    public final String url = "https://docs.google.com/spreadsheets/d/1UjBAX4Z4DCDv4F4zU2JY9RxToAsKMrPDYLwJejmXAW8/edit#gid=0";
    public final Document doc = Jsoup.connect(url).get();
    public final Elements tableElements = doc.select("table");
    public GoogleSheetsScraper() throws IOException {
    }
    public String sheetsParser(String groupString, String dayString) throws IOException {
        if (groupString.matches("^\\d{3}$")) {
            groupString = "11-" + groupString;
        }
        String[] indexArr = indexOfDay(dayString).split(";");
        int startRow = Integer.parseInt(indexArr[0]);
        int finishRow = Integer.parseInt(indexArr[1]);
        List<String> timeTable = new ArrayList<>();
        int flag = 0;
        if (tableElements.size() > 0) {
            int currentRow = 0;
            Element table = tableElements.get(0);
            for (Element row : table.select("tr")) {
                currentRow++;
                Elements cells = row.select("td");
                if (cells.size() > 0) {
                    if (row.elementSiblingIndex() == 1) {
                        for (int i = 0; i < cells.size(); i++) {
                            Element cell = cells.get(i);
                            String cellText = cell.text();
                            if (cellText.replaceAll("[^\\d-]", "").equals(groupString)) {
                                flag = i ;
                                break;
                            }
                        }
                    }
                    if (currentRow >= startRow && currentRow <= finishRow) {
                        String cellTimeText = cells.get(1).text();
                        if (flag != 0 && cellTimeText != "") {
                            Element cell = cells.get(flag);
                            String cellText = cell.text();
                            if (cellText == "") {
                                timeTable.add(cellTimeText + ":   " + "Пары нет, сходи покушай :)");
                            } else {
                                timeTable.add(cellTimeText + ":   " + cellText);
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("Таблица не найдена.");
        }
        return listToString(timeTable);
    }
    public String indexOfDay(String day) {
        switch (day) {
            case "понедельник","пн" : return "3;11";
            case "вторник","вт" : return "12;18";
            case "среда","ср" : return "19;25";
            case "четверг","чт" : return "26;32";
            case "пятница","пт" : return "33;39";
            case "суббота","сб" : return "40;46";
        }
        return null;
    }
    public String listToString(List<String> time) {
        String result = time.get(0) + "\n" + "\n" + time.get(1) + "\n" + "\n"+ time.get(2) + "\n" + "\n" + time.get(3) + "\n" + "\n"+ time.get(4) + "\n" + "\n" + time.get(5) + "\n"+ "\n" + time.get(6) + "\n";
        return result;
    }

}