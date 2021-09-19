package comparator;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;
import ru.yandex.qatools.ashot.shooting.ViewportPastingDecorator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PageComparator {
    private static final String FIRST_PAGE = "_page_1";
    private static final String SECOND_PAGE = "_page_2";
    private static final String DIFF_PAGE = "_diff";
    private static final String HTML_DIFF_PAGE = "_html_diff";
    private static final String PNG_FORMAT = "PNG";
    private static final String TEXT_FORMAT = "TXT";
    private static final String FILE_NAME_EXTENSION = "." + PNG_FORMAT;
    private static final String FILE_NAME_TEXT_EXTENSION = "." + TEXT_FORMAT;
    private static final String BREAK_LINE = "\n";
    private static final String FILE_NAME_PREFIX = String.valueOf(Instant.now().toEpochMilli());


    private static ShootingStrategy shootingStrategy;

    static {
        ShootingStrategy fullWidth = ShootingStrategies.scaling(2);
        shootingStrategy = new ViewportPastingDecorator(fullWidth).withScrollTimeout(300);
    }


    public static void main(String[] args) throws Exception {
        if (!isValidInput(args)) {
            printUsage();
            return;
        }
        String url1 = args[0];
        String url2 = args[1];

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get(url1);
        driver.manage().window().maximize();

        Screenshot screenshot1 = new AShot().shootingStrategy(shootingStrategy).takeScreenshot(driver);
        File file1 = new File(FILE_NAME_PREFIX + FIRST_PAGE + FILE_NAME_EXTENSION);
        ImageIO.write(screenshot1.getImage(), PNG_FORMAT, file1);

        driver.get(url2);
        Screenshot screenshot2 = new AShot().shootingStrategy(shootingStrategy).takeScreenshot(driver);
        File file2 = new File(FILE_NAME_PREFIX + SECOND_PAGE + FILE_NAME_EXTENSION);
        ImageIO.write(screenshot2.getImage(), PNG_FORMAT, file2);


        ImageDiff diff = new ImageDiffer().makeDiff(screenshot1, screenshot2);
        BufferedImage diffImage = diff.getMarkedImage();
        File result = new File(FILE_NAME_PREFIX + DIFF_PAGE + FILE_NAME_EXTENSION);
        ImageIO.write(diffImage, PNG_FORMAT, result);
        driver.quit();

        compareHtmlPage(url1, url2);

        log.info("Screenshot for first page was saved: " + file1.getAbsolutePath());
        log.info("Screenshot for second page was saved: " + file2.getAbsolutePath());
        log.info("Differences result was saved: " + result.getAbsolutePath());
        log.info("Finished!");
        System.exit(0);
    }

    private static String parseUrlToHtml(String url) throws IOException {
        return Jsoup.connect(url).get().toString();
    }

    private static String[] convertHtmlToArray(String htmlAsString) {
        return htmlAsString.split(BREAK_LINE);
    }

    public static void compareHtmlPage(String urlFirstPage, String urlSecondPage) throws IOException {
        try{
            String[] firstPage = convertHtmlToArray(parseUrlToHtml(urlFirstPage));
            String[] secondPage = convertHtmlToArray(parseUrlToHtml(urlSecondPage));
            List<String> htmlDiffs = new ArrayList<>();
            for (int i = 0; i < firstPage.length; i++) {
                if (!firstPage[i].equals(secondPage[i])) {
                    htmlDiffs.add("Html content of 2 pages have difference --->" + " Page 1 is: " + firstPage[i] + " Page 2 is: " + secondPage[i]);
                }
            }

            if(!htmlDiffs.isEmpty()){
                File outputFile = new File(FILE_NAME_PREFIX + HTML_DIFF_PAGE + FILE_NAME_TEXT_EXTENSION);
                FileUtils.writeLines(outputFile, String.valueOf(StandardCharsets.UTF_8), htmlDiffs);
                log.info("Differences html content was saved: " + outputFile.getAbsolutePath());
            }
        }catch (IOException e){
            throw new IOException();
            log.error("Error when compare html content, cause " + e.getStackTrace());
        }
    }

    private static void printUsage() {
        System.out.println(
                "Usage:\tjava -jar compare2pages.jar FIRST_PAGE_URL SECOND_PAGE_URL\n"
        );
    }

    private static boolean isValidInput(String[] input) {
        return (input != null && input.length > 1 && input[0] != null && input[1] != null);
    }
}
