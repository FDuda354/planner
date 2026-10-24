package pl.dudios.planner.dev;

import pl.dudios.planner.transaction.model.Transaction;
import pl.dudios.planner.transaction.model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

class DevTransactionGenerator {

  static final String HOUSING = "HOUSING";
  static final String BILLS = "BILLS";
  static final String FOOD = "FOOD";
  static final String TRANSPORT = "TRANSPORT";
  static final String RESTAURANTS = "RESTAURANTS";
  static final String HEALTH = "HEALTH";
  static final String FUN = "FUN";
  static final String SUBSCRIPTIONS = "SUBSCRIPTIONS";
  static final String CLOTHES = "CLOTHES";
  static final String HOME = "HOME";
  static final String GIFTS = "GIFTS";
  static final String TRAVEL = "TRAVEL";
  static final String SALARY_FILIP = "SALARY_FILIP";
  static final String SALARY_KLAUDIA = "SALARY_KLAUDIA";
  static final String BONUS = "BONUS";
  static final String OTHER_INCOME = "OTHER_INCOME";
  static final String SAFETY_NET = "SAFETY_NET";
  static final String JAPAN = "JAPAN";
  static final String CAR = "CAR";
  static final String CAMERA = "CAMERA";
  static final String BATHROOM = "BATHROOM";

  private static final YearMonth FIRST_MONTH = YearMonth.of(2025, 11);
  private static final String PERSONAL_GROCERY_SHOP = "Żabka";
  private static final Set<String> SHARED_CATEGORIES = Set.of(HOUSING, BILLS, FOOD, SUBSCRIPTIONS, HOME);

  private final DevHousehold household;
  private final Random random = new Random(20251101L);
  private final List<Transaction> transactions = new ArrayList<>();
  private YearMonth month;
  private int lastDay;

  DevTransactionGenerator(DevHousehold household) {
    this.household = household;
  }

  List<Transaction> generate(LocalDate today) {
    YearMonth currentMonth = YearMonth.from(today);
    for (month = FIRST_MONTH; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
      lastDay = month.equals(currentMonth) ? today.getDayOfMonth() : month.lengthOfMonth();
      seedIncomes();
      seedSavings();
      seedFixedExpenses();
      seedVariableExpenses();
      seedSeasonalExpenses();
    }
    return transactions;
  }

  private void seedIncomes() {
    income(SALARY_FILIP, 9200 + between(0, 4) * 50, 10, household.filipId(), "Wynagrodzenie");
    income(SALARY_KLAUDIA, 7400 + between(0, 4) * 50, 10, household.klaudiaId(), "Wynagrodzenie");
    if (month.getMonthValue() % 3 == 0) {
      income(BONUS, between(15, 35) * 100, 20, anyone(), "Premia kwartalna");
    }
    if (chance(0.4)) {
      income(OTHER_INCOME, amount(80, 600), anyDay(), anyone(), pick("Sprzedaż na OLX", "Zwrot podatku", "Zwrot za bilety", "Cashback"));
    }
  }

  private void seedSavings() {
    saving(SAFETY_NET, 1000, 11, household.filipId(), "Stały przelew");
    saving(JAPAN, 600, 11, household.klaudiaId(), "Stały przelew");
    if (!month.isBefore(YearMonth.of(2026, 3))) {
      saving(CAR, 500, 12, household.filipId(), "Stały przelew");
    }
    if (!month.isBefore(YearMonth.of(2026, 5))) {
      saving(CAMERA, 400, 12, household.klaudiaId(), "Odkładam na aparat");
    }
    if (chance(0.3)) {
      saving(JAPAN, between(2, 8) * 100, anyDay(), anyone(), "Dodatkowa wpłata");
    }
  }

  private void seedFixedExpenses() {
    expense(HOUSING, 2850, 10, household.filipId(), "Czynsz i najem");
    expense(BILLS, amount(140, 260), 15, household.filipId(), "Prąd");
    expense(BILLS, 69.99, 5, household.klaudiaId(), "Internet");
    expense(BILLS, 45, 8, household.filipId(), "Telefon");
    expense(BILLS, 45, 8, household.klaudiaId(), "Telefon");
    if (month.getMonthValue() >= 10 || month.getMonthValue() <= 3) {
      expense(BILLS, amount(180, 340), 18, household.klaudiaId(), "Gaz");
    }
    expense(SUBSCRIPTIONS, 60, 3, household.klaudiaId(), "Netflix");
    expense(SUBSCRIPTIONS, 29.99, 7, household.filipId(), "Spotify");
    expense(SUBSCRIPTIONS, 11.99, 14, household.filipId(), "iCloud");
    expense(TRANSPORT, 110, 1, household.klaudiaId(), "Bilet miesięczny");
  }

  private void seedVariableExpenses() {
    repeat(between(12, 17), () -> expense(FOOD, amount(25, 340), anyDay(), anyone(),
      pick("Biedronka", "Lidl", PERSONAL_GROCERY_SHOP, "Auchan", "Warzywniak", "Piekarnia", "Carrefour", "Zakupy na tydzień")));
    repeat(between(3, 5), () -> expense(TRANSPORT, amount(60, 290), anyDay(), anyone(),
      pick("Orlen", "Shell", "BP", "Uber", "Parking", "Myjnia")));
    repeat(between(3, 7), () -> expense(RESTAURANTS, amount(28, 240), anyDay(), anyone(),
      pick("Pizza", "Sushi", "Kebab", "Kawa na mieście", "Obiad w knajpie", "Burgery", "Ramen")));
    repeat(between(0, 2), () -> expense(HEALTH, amount(30, 380), anyDay(), anyone(),
      pick("Apteka", "Dentysta", "Okulista", "Badania krwi", "Fizjoterapia")));
    repeat(between(1, 4), () -> expense(FUN, amount(25, 180), anyDay(), anyone(),
      pick("Kino", "Koncert", "Książka", "Gra planszowa", "Kręgle", "Escape room")));
    repeat(between(0, 2), () -> expense(CLOTHES, amount(80, 420), anyDay(), anyone(),
      pick("Zalando", "Buty", "Kurtka", "Koszulki", "Bluza")));
    repeat(between(1, 3), () -> expense(HOME, amount(30, 260), anyDay(), anyone(),
      pick("Ikea", "Castorama", "Rossmann", "Środki czystości", "Kwiaty do salonu")));
  }

  private void seedSeasonalExpenses() {
    boolean december = month.getMonthValue() == 12;
    repeat(december ? between(4, 6) : between(0, 1), () -> expense(GIFTS, amount(60, 450), december ? between(5, 22) : anyDay(), anyone(),
      pick("Urodziny mamy", "Prezent dla przyjaciół", "Ślub znajomych", "Prezenty świąteczne", "Imieniny taty")));

    if (month.getMonthValue() == 7 || month.getMonthValue() == 8) {
      expense(TRAVEL, amount(2200, 3800), between(1, 10), household.filipId(), pick("Hotel nad morzem", "Apartament w Chorwacji"));
      expense(TRAVEL, amount(400, 900), between(11, 25), household.klaudiaId(), "Atrakcje na wyjeździe");
    } else if (chance(0.25)) {
      expense(TRAVEL, amount(300, 1200), anyDay(), anyone(), pick("Weekend w górach", "Bilety lotnicze", "Nocleg w Krakowie"));
    }
  }

  private void income(String source, double amount, int day, Long memberId, String note) {
    add(TransactionType.INCOME, household.income(source), amount, day, memberId, false, note);
  }

  private void saving(String goal, double amount, int day, Long memberId, String note) {
    add(TransactionType.SAVING, household.goal(goal), amount, day, memberId, false, note);
  }

  private void expense(String category, double amount, int day, Long memberId, String note) {
    boolean shared = SHARED_CATEGORIES.contains(category) && !PERSONAL_GROCERY_SHOP.equals(note);
    add(TransactionType.EXPENSE, household.category(category), amount, day, memberId, shared, note);
  }

  private void add(TransactionType type, Long itemId, double amount, int day, Long memberId, boolean shared, String note) {
    if (day > lastDay) {
      return;
    }
    Transaction transaction = Transaction.builder()
                                         .householdId(household.householdId())
                                         .amount(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP))
                                         .date(month.atDay(day))
                                         .memberId(memberId)
                                         .shared(shared)
                                         .note(note)
                                         .build();
    transaction.assignItem(type, itemId);
    transactions.add(transaction);
  }

  private void repeat(int times, Runnable action) {
    for (int i = 0; i < times; i++) {
      action.run();
    }
  }

  private int anyDay() {
    return between(1, month.lengthOfMonth());
  }

  private Long anyone() {
    return chance(0.5) ? household.filipId() : household.klaudiaId();
  }

  private int between(int min, int max) {
    return min + random.nextInt(max - min + 1);
  }

  private double amount(double min, double max) {
    return min + random.nextDouble() * (max - min);
  }

  private boolean chance(double probability) {
    return random.nextDouble() < probability;
  }

  private String pick(String... values) {
    return values[random.nextInt(values.length)];
  }

}
