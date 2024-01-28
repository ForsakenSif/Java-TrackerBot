package io.proj3ct.SpringEduBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.SpringEduBot.model.Cards;
import io.proj3ct.SpringEduBot.model.CardsRepository;
import io.proj3ct.SpringEduBot.model.User;
import io.proj3ct.SpringEduBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;

import io.proj3ct.SpringEduBot.config.BotConfig;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot{

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CardsRepository cardsRepository;
	final BotConfig config;
	static final String HELP_TEXT = "This bot is created to Track the Wildberries products.\n\n" +
			"You can execute commands from the main menu on the left or by typing a command:\n\n" +
			"Type /start to see a welcome message\n\n"+
			"Type /help to see this message again\n\n";

	public TelegramBot(BotConfig config) {
		this.config = config;
		List<BotCommand> listofCommands = new ArrayList<>();
//		listofCommands.add(new BotCommand("/start", "get a welcome message!"));
		listofCommands.add(new BotCommand("/menu","Меню управления добавленными товарами"));
		listofCommands.add(new BotCommand("/help", "Помощь"));


		try{
			this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
		} catch (TelegramApiException e){
			log.error("Error setting bot's command list: " + e.getMessage());
		}
	}


	@Override
	public String getBotUsername() {
		// TODO Auto-generated method stub
		return config.getBotName();
	}

	@Override
	public String getBotToken() {
		// TODO Auto-generated method stub
		return config.getToken();
	}

	ArrayList<String> sizeList = new ArrayList<>();

	@Override
	public void onUpdateReceived(Update update) {

		if(update.hasMessage() && update.getMessage().hasText()) {
			String messageText = update.getMessage().getText();
			long chatId = update.getMessage().getChatId();


			if(messageText.contains("/send") && config.getOwnerId() == chatId) { // Admin command
				var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
				var users = userRepository.findAll();
				for(User user: users) {
					sendMessage(user.getChatId(), textToSend);
				}
			} else if (messageText.contains("/add")) { // old method to add new card
				addNewCard(update.getMessage());
			} else {


				switch(messageText) {
					case "/start": // Meeting

						registerUser(update.getMessage());
						startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
						break;

					case "/help":

						sendMessage(chatId, EmojiParser.parseToUnicode(HELP_TEXT));
						break;

//					case "/register":
//
//						register(chatId);
//						break;

					case "/menu": // Menu with all user's cards

						var cards = cardsRepository.findAll();
						List<Cards> userCards = new ArrayList<>();
						for(Cards card: cards){
							if(card.getChatId() == chatId){
								userCards.add(card);

							}
						}
						if(userCards.isEmpty()){
//							sendMessage(chatId,"You have no cards, time to add some =)");
							sendMessage(chatId,"На данный момент у вас нет отслеживаемых товаров, время добавить парочку =)");
						} else {
							SendMessage message = new SendMessage();
							message.setChatId(String.valueOf(chatId));
//							message.setText("This is your cards list: \n\n");
							message.setText("Список отслеживаемых товаров: \n\n");


							InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
							List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

							for(Cards userCard: userCards){
								List<InlineKeyboardButton> rowInLine = new ArrayList<>();
								String shortProductName;
								if(userCard.getProductName().length() < 20) {
									shortProductName = userCard.getProductName();
								}else{
									shortProductName = userCard.getProductName().substring(0,20) + "...";
								}

								var cardButton = new InlineKeyboardButton();

								if(userCard.getStock() == 0){
									if(userCard.getSizeName() == null) {
										cardButton.setText(EmojiParser.parseToUnicode(":x: " + userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));
									} else {
										cardButton.setText(EmojiParser.parseToUnicode(userCard.getSizeName() + " | :x: " + userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));
									}


								} else {
									if(userCard.getSizeName() == null){
										cardButton.setText(EmojiParser.parseToUnicode(userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));
									} else {
										cardButton.setText(EmojiParser.parseToUnicode(userCard.getSizeName() + " | " + userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));

									}
								}
								cardButton.setCallbackData(String.valueOf(userCard.getId()));
								rowInLine.add(cardButton);
								rowsInLine.add(rowInLine);


							}
							List<InlineKeyboardButton> rowInLine = new ArrayList<>();

							var deleteAllButton = new InlineKeyboardButton();
							deleteAllButton.setText(EmojiParser.parseToUnicode(":name_badge: Удалить всё :name_badge:"));
							deleteAllButton.setCallbackData("DELETE_ALL_CARDS_BUTTON");

							var testButton = new InlineKeyboardButton(); // Useless button btw
							testButton.setText(EmojiParser.parseToUnicode("Created by @forsakenSif"));
							testButton.setCallbackData("TEST_BUTTON");

							rowInLine.add(deleteAllButton);
							rowInLine.add(testButton);
							rowsInLine.add(rowInLine);

							markupInLine.setKeyboard(rowsInLine);
							message.setReplyMarkup(markupInLine);


							try {
								execute(message);
							} catch (TelegramApiException e) {

								log.error("Error occurred: " + e.getMessage());

							}
						}
						break;


					default:
						addNewCard(update.getMessage()); // all messages are perceived as adding a new card
//						sendMessage(chatId, "Use the '/add (article)' command to add a new Wildberries card");
//						sendMessage(chatId, "Используй команду '/add (артикул)', чтобы отслеживать цену на товар.");

				}
			}

		} else if (update.hasCallbackQuery()) { // InlineKeyBoard buttons processing
			String callbackData = update.getCallbackQuery().getData();
			long messageId = update.getCallbackQuery().getMessage().getMessageId();
			long chatId = update.getCallbackQuery().getMessage().getChatId();

			for(String size:sizeList) {
				if (callbackData.equals(size)){
					EditMessageText message = new EditMessageText();
					message.setParseMode("Markdown");
					message.enableMarkdown(true);
					message.disableWebPagePreview();
					message.setChatId(String.valueOf(chatId));

					try{

						String article = update.getCallbackQuery().getMessage().getText().substring(update.getCallbackQuery().getMessage().getText().indexOf("Артикул: ")+9);
						URL url = new URL("https://card.wb.ru/cards/detail?appType=128&curr=byn&lang=ru&dest=-59208&regions=1,4,22,30,31,33,40,48,66,68,69,70,80,83,114&spp=32&nm=" + article + "&uclusters=1");
						String stringUrl = "https://card.wb.ru/cards/detail?appType=128&curr=byn&lang=ru&dest=-59208&regions=1,4,22,30,31,33,40,48,66,68,69,70,80,83,114&spp=32&nm=" + article + "&uclusters=1";
						BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
						String line;
						StringBuilder jsonString =new StringBuilder();
						while((line = reader.readLine()) != null){
							jsonString.append(line);
						}
						reader.close();
						JSONParser parser = new JSONParser();

						JSONObject rootJsonObject = (JSONObject)parser.parse(jsonString.toString());
						JSONObject data = (JSONObject) rootJsonObject.get("data");
						JSONArray products = (JSONArray) data.get("products");
						if(products.isEmpty()){
							sendMessage(chatId,"Invalid article");
						} else {


							JSONObject product = (JSONObject)products.get(0);
							String name = (String)product.get("name");
							String brandName = (String) product.get("brand");
							Long fullPrice = (Long)product.get("salePriceU");
							Double price = fullPrice.doubleValue()/100;
							Double rating = Double.valueOf(String.valueOf(product.get("reviewRating")));
							Long feedbackCount = (Long) product.get("feedbacks");

							JSONArray sizes = (JSONArray) product.get("sizes");
							Long currentStock = 0L;
							for (int i = 0; i < sizes.size(); i++){


								JSONObject sizeList = (JSONObject) sizes.get(i);
								String sizeName = (String) sizeList.get("origName");
								if (Objects.equals(size, sizeName)){
									JSONArray stocks = (JSONArray) sizeList.get("stocks");
									if(!stocks.isEmpty()){
										for(int j=0; j  <  stocks.size(); j++){

											JSONObject stock = (JSONObject) stocks.get(j);
											currentStock = currentStock + (Long) stock.get("qty");

										}

									}}


							}


							message.setText(EmojiParser.parseToUnicode(":mag_right: Товар отслеживается :mag_right:\n\n\n" +
									":fire:*" + size + " | " + name + "*:fire: \n" +
									"От бренда *" + brandName + "*\n\n" +
									":dollar: Цена: *" + price + "* BYN\n\n" +
									":sparkles: Артикул: `" + article + "`\n\n" +
									":star: Рейтинг: *" + rating + "*\n" +
									":notebook: Количество отзывов: *" + feedbackCount + "*\n\n" +
									":low_brightness: Количество товара: " + currentStock));

							message.setMessageId((int)messageId);
							try {
								execute(message);
							} catch (TelegramApiException e) {

								log.error("Error occurred: " + e.getMessage());

							}


							Cards card = new Cards();

							card.setChatId(chatId);
							card.setSizeName(size);
							card.setBrandName(brandName);
							card.setRating(rating);
							card.setFeedbackCount(feedbackCount);
							card.setProductName(name);
							card.setPrice(price);
							card.setArticle(article);
							card.setLink(stringUrl);
							card.setStock(currentStock);
							card.setPriceNotification(TRUE);
							card.setAmountNotification(TRUE);

							card.setAddedAt(new Timestamp(System.currentTimeMillis()));
							cardsRepository.save(card);
							log.info("card saved: " + card);

						}
					} catch (Exception e){
						e.printStackTrace();
						sendMessage(chatId,"Error");
					}

				}
			}

			var userCards = cardsRepository.findAll();
			for(Cards userCard: userCards) {
				if(callbackData.equals(String.valueOf(userCard.getId()))) {
					String text;
					if (userCard.getBrandName().isEmpty()) {
						text = EmojiParser.parseToUnicode(":fire:" + "[" + userCard.getProductName() + "](https://www.wildberries.by/product?card=" + userCard.getArticle() + "):fire: \n\n" +
								":dollar: Цена: *" + userCard.getPrice() + "* BYN\n\n" +
								":sparkles: Артикул: `" + userCard.getArticle() + "`\n\n" +
								":star: Рейтинг: *" + userCard.getRating() + "*\n" +
								":notebook: Количество отзывов: *" + userCard.getFeedbackCount() + "*\n\n" +
								":white_medium_square: Количество товара: " + userCard.getStock());
					} else {
						text = EmojiParser.parseToUnicode(":fire:" + "[" + userCard.getProductName() + "](https://www.wildberries.by/product?card=" + userCard.getArticle() + "):fire: \n" +
								"От бренда *" + userCard.getBrandName() + "*\n\n" +
								":dollar: Цена: *" + userCard.getPrice() + "* BYN\n\n" +
								":sparkles: Артикул: `" + userCard.getArticle() + "`\n\n" +
								":star: Рейтинг: *" + userCard.getRating() + "*\n" +
								":notebook: Количество отзывов: *" + userCard.getFeedbackCount() + "*\n\n" +
								":low_brightness: Количество товара: " + userCard.getStock());
					}
					EditMessageText message = new EditMessageText();


					InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
					List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
					List<InlineKeyboardButton> rowInLine = new ArrayList<>();
					var deleteButton = new InlineKeyboardButton();
//					deleteButton.setText("Delete");
					deleteButton.setText(EmojiParser.parseToUnicode(":name_badge: Удалить :name_badge:"));
					deleteButton.setCallbackData("DELETE_BUTTON" + userCard.getId());


					var backButton = new InlineKeyboardButton();
//					backButton.setText("Go back");
					backButton.setText(EmojiParser.parseToUnicode("Вернуться"));
					backButton.setCallbackData("BACK_BUTTON");

					rowInLine.add(deleteButton);
					rowInLine.add(backButton);


					List<InlineKeyboardButton> secondRowInLine = new ArrayList<>();

					var soundNotificationButton = new InlineKeyboardButton();
					if(userCard.getPriceNotification() == TRUE) {
						soundNotificationButton.setText(EmojiParser.parseToUnicode(":bell: Изменение цены"));
					} else{
						soundNotificationButton.setText(EmojiParser.parseToUnicode(":no_bell: Изменение цены"));
					}
					soundNotificationButton.setCallbackData("SOUND_NOTIFICATION_BUTTON"+userCard.getId());

					var amountNotificationButton = new InlineKeyboardButton();
					if(userCard.getAmountNotification() == TRUE){
						amountNotificationButton.setText(EmojiParser.parseToUnicode("Наличие товара :bell:"));
					} else {
						amountNotificationButton.setText(EmojiParser.parseToUnicode("Наличие товара :no_entry_sign:"));
					}

					amountNotificationButton.setCallbackData("AMOUNT_NOTIFICATION_BUTTON"+userCard.getId());

					secondRowInLine.add(soundNotificationButton);
					secondRowInLine.add(amountNotificationButton);

					rowsInLine.add(secondRowInLine);
					rowsInLine.add(rowInLine);

					markupInLine.setKeyboard(rowsInLine);

					message.setReplyMarkup(markupInLine);

					message.enableMarkdown(true);
					message.disableWebPagePreview();
					message.setChatId(String.valueOf(chatId));
					message.setText(text);
					message.setMessageId((int)messageId);
					try {
						execute(message);
					} catch (TelegramApiException e) {

						log.error("Error occurred: " + e.getMessage());

					}
				}

				if(callbackData.equals("SOUND_NOTIFICATION_BUTTON" + userCard.getId())) {
					String text;
					if (userCard.getBrandName().isEmpty()) {
						text = EmojiParser.parseToUnicode(":fire:" + "[" + userCard.getProductName() + "](https://www.wildberries.by/product?card=" + userCard.getArticle() + "):fire: \n\n" +
								":dollar: Цена: *" + userCard.getPrice() + "* BYN\n\n" +
								":sparkles: Артикул: `" + userCard.getArticle() + "`\n\n" +
								":star: Рейтинг: *" + userCard.getRating() + "*\n" +
								":notebook: Количество отзывов: *" + userCard.getFeedbackCount() + "*\n\n" +
								":white_medium_square: Количество товара: " + userCard.getStock());
					} else {
						text = EmojiParser.parseToUnicode(":fire:" + "[" + userCard.getProductName() + "](https://www.wildberries.by/product?card=" + userCard.getArticle() + "):fire: \n" +
								"От бренда *" + userCard.getBrandName() + "*\n\n" +
								":dollar: Цена: *" + userCard.getPrice() + "* BYN\n\n" +
								":sparkles: Артикул: `" + userCard.getArticle() + "`\n\n" +
								":star: Рейтинг: *" + userCard.getRating() + "*\n" +
								":notebook: Количество отзывов: *" + userCard.getFeedbackCount() + "*\n\n" +
								":low_brightness: Количество товара: " + userCard.getStock());
					}
					EditMessageText message = new EditMessageText();
					if(userCard.getPriceNotification() == TRUE){
						userCard.setPriceNotification(FALSE);
					} else {
						userCard.setPriceNotification(TRUE);
					}
					cardsRepository.save(userCard);


					InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
					List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
					List<InlineKeyboardButton> rowInLine = new ArrayList<>();
					var deleteButton = new InlineKeyboardButton();
//					deleteButton.setText("Delete");
					deleteButton.setText(EmojiParser.parseToUnicode(":name_badge: Удалить :name_badge:"));
					deleteButton.setCallbackData("DELETE_BUTTON" + userCard.getId());


					var backButton = new InlineKeyboardButton();
//					backButton.setText("Go back");
					backButton.setText(EmojiParser.parseToUnicode("Вернуться"));
					backButton.setCallbackData("BACK_BUTTON");

					rowInLine.add(deleteButton);
					rowInLine.add(backButton);


					List<InlineKeyboardButton> secondRowInLine = new ArrayList<>();

					var soundNotificationButton = new InlineKeyboardButton();
					if(userCard.getPriceNotification() == TRUE) {
						soundNotificationButton.setText(EmojiParser.parseToUnicode(":bell: Изменение цены"));
					} else{
						soundNotificationButton.setText(EmojiParser.parseToUnicode(":no_bell: Изменение цены"));
					}
					soundNotificationButton.setCallbackData("SOUND_NOTIFICATION_BUTTON"+userCard.getId());

					var amountNotificationButton = new InlineKeyboardButton();
					if(userCard.getAmountNotification() == TRUE){
						amountNotificationButton.setText(EmojiParser.parseToUnicode("Наличие товара :bell:"));
					} else {
						amountNotificationButton.setText(EmojiParser.parseToUnicode("Наличие товара :no_entry_sign:"));
					}

					amountNotificationButton.setCallbackData("AMOUNT_NOTIFICATION_BUTTON"+userCard.getId());

					secondRowInLine.add(soundNotificationButton);
					secondRowInLine.add(amountNotificationButton);

					rowsInLine.add(secondRowInLine);
					rowsInLine.add(rowInLine);

					markupInLine.setKeyboard(rowsInLine);

					message.setReplyMarkup(markupInLine);

					message.enableMarkdown(true);
					message.disableWebPagePreview();
					message.setChatId(String.valueOf(chatId));
					message.setText(text);
					message.setMessageId((int)messageId);
					try {
						execute(message);
					} catch (TelegramApiException e) {

						log.error("Error occurred: " + e.getMessage());

					}
				}
				if(callbackData.equals("AMOUNT_NOTIFICATION_BUTTON" + userCard.getId())) {
					String text;
					if (userCard.getBrandName().isEmpty()) {
						text = EmojiParser.parseToUnicode(":fire:" + "[" + userCard.getProductName() + "](https://www.wildberries.by/product?card=" + userCard.getArticle() + "):fire: \n\n" +
								":dollar: Цена: *" + userCard.getPrice() + "* BYN\n\n" +
								":sparkles: Артикул: `" + userCard.getArticle() + "`\n\n" +
								":star: Рейтинг: *" + userCard.getRating() + "*\n" +
								":notebook: Количество отзывов: *" + userCard.getFeedbackCount() + "*\n\n" +
								":white_medium_square: Количество товара: " + userCard.getStock());
					} else {
						text = EmojiParser.parseToUnicode(":fire:" + "[" + userCard.getProductName() + "](https://www.wildberries.by/product?card=" + userCard.getArticle() + "):fire: \n" +
								"От бренда *" + userCard.getBrandName() + "*\n\n" +
								":dollar: Цена: *" + userCard.getPrice() + "* BYN\n\n" +
								":sparkles: Артикул: `" + userCard.getArticle() + "`\n\n" +
								":star: Рейтинг: *" + userCard.getRating() + "*\n" +
								":notebook: Количество отзывов: *" + userCard.getFeedbackCount() + "*\n\n" +
								":low_brightness: Количество товара: " + userCard.getStock());
					}
					EditMessageText message = new EditMessageText();
					if(userCard.getAmountNotification() == TRUE){
						userCard.setAmountNotification(FALSE);
					} else {
						userCard.setAmountNotification(TRUE);
					}
					cardsRepository.save(userCard);


					InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
					List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
					List<InlineKeyboardButton> rowInLine = new ArrayList<>();
					var deleteButton = new InlineKeyboardButton();
//					deleteButton.setText("Delete");
					deleteButton.setText(EmojiParser.parseToUnicode(":name_badge: Удалить :name_badge:"));
					deleteButton.setCallbackData("DELETE_BUTTON" + userCard.getId());


					var backButton = new InlineKeyboardButton();
//					backButton.setText("Go back");
					backButton.setText(EmojiParser.parseToUnicode("Вернуться"));
					backButton.setCallbackData("BACK_BUTTON");

					rowInLine.add(deleteButton);
					rowInLine.add(backButton);


					List<InlineKeyboardButton> secondRowInLine = new ArrayList<>();

					var soundNotificationButton = new InlineKeyboardButton();
					if(userCard.getPriceNotification() == TRUE) {
						soundNotificationButton.setText(EmojiParser.parseToUnicode(":bell: Изменение цены"));
					} else{
						soundNotificationButton.setText(EmojiParser.parseToUnicode(":no_bell: Изменение цены"));
					}
					soundNotificationButton.setCallbackData("SOUND_NOTIFICATION_BUTTON"+userCard.getId());

					var amountNotificationButton = new InlineKeyboardButton();
					if(userCard.getAmountNotification() == TRUE){
						amountNotificationButton.setText(EmojiParser.parseToUnicode("Наличие товара :bell:"));
					} else {
						amountNotificationButton.setText(EmojiParser.parseToUnicode("Наличие товара :no_entry_sign:"));
					}

					amountNotificationButton.setCallbackData("AMOUNT_NOTIFICATION_BUTTON"+userCard.getId());

					secondRowInLine.add(soundNotificationButton);
					secondRowInLine.add(amountNotificationButton);

					rowsInLine.add(secondRowInLine);
					rowsInLine.add(rowInLine);

					markupInLine.setKeyboard(rowsInLine);

					message.setReplyMarkup(markupInLine);

					message.enableMarkdown(true);
					message.disableWebPagePreview();
					message.setChatId(String.valueOf(chatId));
					message.setText(text);
					message.setMessageId((int)messageId);
					try {
						execute(message);
					} catch (TelegramApiException e) {

						log.error("Error occurred: " + e.getMessage());

					}
				}
			} if (callbackData.equals("BACK_BUTTON")){
				EditMessageText message = new EditMessageText();
				message.setChatId(String.valueOf(chatId));
				message.setMessageId((int)messageId);
				message.setText("Список отслеживаемых товаров: \n\n");


				InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
				List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

				for(Cards userCard: userCards){
					if(userCard.getChatId() == chatId){
						List<InlineKeyboardButton> rowInLine = new ArrayList<>();

						String shortProductName;
						if(userCard.getProductName().length() < 20) {
							shortProductName = userCard.getProductName();
						}else{
							shortProductName = userCard.getProductName().substring(0, 20) + "...";
						}

						var cardButton = new InlineKeyboardButton();
						if(userCard.getStock() == 0){
							if(userCard.getSizeName() == null) {
								cardButton.setText(EmojiParser.parseToUnicode(":x: " + userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));
							} else {
								cardButton.setText(EmojiParser.parseToUnicode(userCard.getSizeName() + " | :x: " + userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));
							}


						} else {
							if(userCard.getSizeName() == null){
								cardButton.setText(EmojiParser.parseToUnicode(userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));
							} else {
								cardButton.setText(EmojiParser.parseToUnicode(userCard.getSizeName() + " | " + userCard.getPrice() + " BYN :dollar: " + shortProductName + " :star: " + userCard.getRating()));

							}
						}
						cardButton.setCallbackData(String.valueOf(userCard.getId()));
						rowInLine.add(cardButton);
						rowsInLine.add(rowInLine);}


				}
				List<InlineKeyboardButton> rowInLine = new ArrayList<>();

				var deleteAllButton = new InlineKeyboardButton();
				deleteAllButton.setText(EmojiParser.parseToUnicode(":name_badge: Удалить всё :name_badge:"));
				deleteAllButton.setCallbackData("DELETE_ALL_CARDS_BUTTON");

				var testButton = new InlineKeyboardButton();
				testButton.setText(EmojiParser.parseToUnicode("Created by @forsakenSif"));
				testButton.setCallbackData("TEST_BUTTON");

				rowInLine.add(deleteAllButton);
				rowInLine.add(testButton);
				rowsInLine.add(rowInLine);

				markupInLine.setKeyboard(rowsInLine);
				message.setReplyMarkup(markupInLine);


				try {
					execute(message);
				} catch (TelegramApiException e) {

					log.error("Error occurred: " + e.getMessage());

				}

			}
			if(callbackData.equals("DELETE_ALL_CARDS_BUTTON")){
				String text = EmojiParser.parseToUnicode(":name_badge: УДАЛИТЬ ВСЕ КАРТОЧКИ? :name_badge:");

				InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
				List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
				List<InlineKeyboardButton> rowInLine = new ArrayList<>();
				var confirmButton = new InlineKeyboardButton();
				confirmButton.setText(EmojiParser.parseToUnicode("ДА"));
				confirmButton.setCallbackData("CONFIRM_ALL_DELETE");
				var backButton = new InlineKeyboardButton();
				backButton.setText(EmojiParser.parseToUnicode("НЕТ"));
				backButton.setCallbackData("BACK_BUTTON");
				rowInLine.add(confirmButton);
				rowInLine.add(backButton);
				rowsInLine.add(rowInLine);
				markupInLine.setKeyboard(rowsInLine);
				EditMessageText message = new EditMessageText();
				message.setReplyMarkup(markupInLine);


				message.setChatId(String.valueOf(chatId));
				message.setText(text);
				message.setMessageId((int)messageId);
				try {
					execute(message);
				} catch (TelegramApiException e) {

					log.error("Error occurred: " + e.getMessage());

				}


			} else if (callbackData.equals("CONFIRM_ALL_DELETE")) {
				for (Cards card: userCards){
					if(card.getChatId() == chatId){
						cardsRepository.delete(card);
					}
				}
				String text = EmojiParser.parseToUnicode(":name_badge: ВСЕ КАРТОЧКИ УДАЛЕНЫ :name_badge:");
				EditMessageText message = new EditMessageText();
				message.setChatId(String.valueOf(chatId));
				message.setText(text);
				message.setMessageId((int)messageId);
				try {
					execute(message);
				} catch (TelegramApiException e) {

					log.error("Error occurred: " + e.getMessage());


				}

			} for(Cards userCard: userCards) {

				if (callbackData.equals("DELETE_BUTTON"+ userCard.getId())) {
					EditMessageText message = new EditMessageText();
					message.setChatId(String.valueOf(chatId));
					message.setMessageId((int) messageId);
					message.setText("Deleted");
					cardsRepository.delete(userCard);
					try {
						execute(message);
					} catch (TelegramApiException e) {

						log.error("Error occurred: " + e.getMessage());


					}
				}
			}


		}

	}


	private void addNewCard(Message msg) {
		if(cardsRepository.findById(msg.getChatId()).isEmpty()){


			var chatId = msg.getChatId();
			String chatText = msg.getText();
			String article;
			if(chatText.contains("card=") && chatText.contains("&")){
				article = chatText.substring(chatText.indexOf("card=")+5,chatText.indexOf("&"));} else if (chatText.contains("card=")) {
				article = chatText.substring(chatText.indexOf("card=")+5);
			} else {
				article = chatText.substring(chatText.indexOf(" ") + 1);
			}
			try{
				URL url = new URL("https://card.wb.ru/cards/detail?appType=128&curr=byn&lang=ru&dest=-59208&regions=1,4,22,30,31,33,40,48,66,68,69,70,80,83,114&spp=32&nm=" + article + "&uclusters=1");
				String stringUrl = "https://card.wb.ru/cards/detail?appType=128&curr=byn&lang=ru&dest=-59208&regions=1,4,22,30,31,33,40,48,66,68,69,70,80,83,114&spp=32&nm=" + article + "&uclusters=1";
				BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
				String line;
				StringBuilder jsonString =new StringBuilder();
				while((line = reader.readLine()) != null){
					jsonString.append(line);
				}
				reader.close();
				JSONParser parser = new JSONParser();

				JSONObject rootJsonObject = (JSONObject)parser.parse(jsonString.toString());
				JSONObject data = (JSONObject) rootJsonObject.get("data");
				JSONArray products = (JSONArray) data.get("products");
				if(products.isEmpty()){
					sendMessage(chatId,"Invalid article");
				} else {


					JSONObject product = (JSONObject)products.get(0);
					String name = (String)product.get("name");
					String brandName = (String) product.get("brand");
					Long fullPrice = (Long)product.get("salePriceU");
					Double price = fullPrice.doubleValue()/100;
					Double rating = Double.valueOf(String.valueOf(product.get("reviewRating")));
					Long feedbackCount = (Long) product.get("feedbacks");
// v.2

					boolean sizesIsEmpty = false;
					InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
					List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
					List<InlineKeyboardButton> rowInLine = new ArrayList<>();
					List<InlineKeyboardButton> secondRowInLine = new ArrayList<>();
					List<InlineKeyboardButton> thirdRowInLine = new ArrayList<>();
//

//
//						sizeButton.setText(EmojiParser.parseToUnicode(sizeName + " | " + currentStock));
//						sizeButton.setCallbackData("SIZE" + userCard.getId());
//						rowInLine.add(sizeButton);
					JSONArray sizes = (JSONArray) product.get("sizes");
					Long stockCount = 0L;
					for (int i = 0; i < sizes.size(); i++){
						Long currentStock = 0L;

						JSONObject size = (JSONObject) sizes.get(i);
						String sizeName = (String) size.get("origName");
						if(Objects.equals(sizeName, "0")) { sizesIsEmpty = true;}
						JSONArray stocks = (JSONArray) size.get("stocks");
						if(!stocks.isEmpty()){
							for(int j=0; j  <  stocks.size(); j++){

								JSONObject stock = (JSONObject) stocks.get(j);
								stockCount = stockCount + (Long) stock.get("qty");
								currentStock = currentStock + (Long) stock.get("qty");

							}

						}
						if(sizesIsEmpty == false) {

							if(!sizeList.contains(sizeName)){sizeList.add(sizeName);}

							var sizeButton = new InlineKeyboardButton();
							if (currentStock != 0) {
								sizeButton.setText(sizeName);
							} else { sizeButton.setText(EmojiParser.parseToUnicode(":x: " + sizeName)); }
							sizeButton.setCallbackData(sizeName);
							if(rowInLine.size() <= 4) {
								rowInLine.add(sizeButton);
							} else if (secondRowInLine.size() <= 4) {
								secondRowInLine.add(sizeButton);
							} else {
								thirdRowInLine.add(sizeButton);
							}
						}


					}
					if(sizesIsEmpty == false){
						rowsInLine.add(rowInLine);
						if (!secondRowInLine.isEmpty()) { rowsInLine.add(secondRowInLine); }
						if (!thirdRowInLine.isEmpty()) { rowsInLine.add(thirdRowInLine); }
						markupInLine.setKeyboard(rowsInLine);
						SendMessage message = new SendMessage();
						message.setParseMode("Markdown");
						message.enableMarkdown(true);
						message.disableWebPagePreview();
						message.setChatId(String.valueOf(chatId));
						message.setChatId(String.valueOf(chatId));
						message.setText(EmojiParser.parseToUnicode(":fire:*" + name + "*:fire: \n" +
								"От бренда *" + brandName + "*\n\n" +
								":dollar: Цена: *" + price + "* BYN\n\n" +
								":sparkles: Артикул: " + article));
						message.setReplyMarkup(markupInLine);
						try {
							execute(message);
						} catch (TelegramApiException e) {

							log.error("Error occurred: " + e.getMessage());

						}
					} else {

						sendMessage(chatId,EmojiParser.parseToUnicode(":mag_right: Товар отслеживается :mag_right:\n\n\n" +
								":fire:*" + name + "*:fire: \n" +
								"От бренда *" + brandName + "*\n\n" +
								":dollar: Цена: *" + price + "* BYN\n\n" +
								":sparkles: Артикул: `" + article + "`\n\n" +
								":star: Рейтинг: *" + rating + "*\n" +
								":notebook: Количество отзывов: *" + feedbackCount + "*\n\n" +
								":low_brightness: Количество товара: " + stockCount));

//					InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
//					List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
//					List<InlineKeyboardButton> rowInLine = new ArrayList<>();
//					var addButton = new InlineKeyboardButton();
//					addButton.setText(EmojiParser.parseToUnicode(":mag_right: Отслеживать :mag_right:"));
//					addButton.setCallbackData("ADD_NEW_CARD");

						Cards card = new Cards();

						card.setChatId(chatId);
						card.setBrandName(brandName);
						card.setRating(rating);
						card.setFeedbackCount(feedbackCount);
						card.setProductName(name);
						card.setPrice(price);
						card.setArticle(article);
						card.setLink(stringUrl);
						card.setStock(stockCount);
						card.setPriceNotification(TRUE);
						card.setAmountNotification(TRUE);

						card.setAddedAt(new Timestamp(System.currentTimeMillis()));
						cardsRepository.save(card);
						log.info("card saved: " + card);
//					sendMessage(chatId,"New card added! - " + name);
//					sendMessage(chatId,"Товар добавлен - " + name);
					}}
			} catch (Exception e){
				e.printStackTrace();
				sendMessage(chatId,"Error");
			}


		}
	}

	private void register(long chatId) {

		SendMessage message = new SendMessage();
		message.setChatId(String.valueOf(chatId));
		message.setText("Do you really want to register?");

		InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
		List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
		List<InlineKeyboardButton> rowInLine = new ArrayList<>();
		var yesButton = new InlineKeyboardButton();
		yesButton.setText("Yes! =)");
		yesButton.setCallbackData("YES_BUTTON");

		var noButton = new InlineKeyboardButton();
		noButton.setText("No =(");
		noButton.setCallbackData("NO_BUTTON");

		rowInLine.add(yesButton);
		rowInLine.add(noButton);

		rowsInLine.add(rowInLine);

		markupInLine.setKeyboard(rowsInLine);

		message.setReplyMarkup(markupInLine);

		try {
			execute(message);
		} catch (TelegramApiException e) {

			log.error("Error occurred: " + e.getMessage());

		}


	}

	private void registerUser(Message msg) {

		if(userRepository.findById(msg.getChatId()).isEmpty()){

			var chatId = msg.getChatId();
			var chat = msg.getChat();

			User user = new User();

			user.setChatId(chatId);
			user.setFirstName(chat.getFirstName());
			user.setLastName(chat.getLastName());
			user.setUserName(chat.getUserName());
			user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

			userRepository.save(user);
			log.info("user saved: " + user);
		}

	}

	private void startCommandReceived(long chatId, String name){


//		String answer = EmojiParser.parseToUnicode(":fire:" + " Hi, " + name + ", nice to meet you! This is test WildberriesTracker bot by Anatoliy " + ":fire: \n\nUse command '/add (article)' to add new Wildberries card!");
		String answer = EmojiParser.parseToUnicode(":fire:" + " Привет, " + name + ". Этот бот позволяет отслеживать цены на товары с Wildberries. :fire:\n\n" +
				":speech_balloon: *Что умеет бот?*\n" +
				"- Бот позволяет отслеживать изменение цены на товар. Уведомляет о том, что товар закончился, или появился в продаже.\n\n" +
				"- Вы можете сами настраивать необходимые вам уведомление в /menu нажав на соответствующий товар.\n\n" +
				"- Бот также предоставляет информацию о *Рейтинге*, *Количестве отзывов* и *Количестве товара*.\n\n\n" +
				":speech_balloon: *Как добавить товар?*\n" +
				"- Достаточно лишь скинуть артикул/ссылку на товар. Можно добавлять товары через 'поделиться' в приложении Wildberries.\n\n" +
				":bangbang: На данный момент все цены указаны с учётом максимально возможной скидки покупателя и могут отличаться от тех, что вы видите у себя на сайте.");
		// String answer = "Hi, " + name + ", nice to meet you! I was created by Anatoliy =D";

		log.info("Replied to user " + name);

		sendMessage(chatId, answer);
	}

	private void sendMessage(long chatId, String textToSend){
		SendMessage message = new SendMessage();
		message.setParseMode("Markdown");
		message.enableMarkdown(true);
		message.disableWebPagePreview();
		message.setChatId(String.valueOf(chatId));
		message.setText(textToSend);


		//Keyborard
//		ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//		keyboardMarkup.setResizeKeyboard(true);
//
//		List<KeyboardRow> keyboardRows = new ArrayList<>();
//
//		KeyboardRow row = new KeyboardRow();
//		row.add("weather");
//		row.add("get random joke");
//		keyboardRows.add(row);
//
//		row = new KeyboardRow();
//		row.add("register");
//		row.add("check my data");
//		row.add("delete my data");
//		keyboardRows.add(row);
//
//		keyboardMarkup.setKeyboard(keyboardRows);
//		message.setReplyMarkup(keyboardMarkup);


		try {
			execute(message);
		} catch (TelegramApiException e) {

			log.error("Error occurred: " + e.getMessage());

		}

	}

	private void sendMessageNoSound(long chatId, String textToSend){
		SendMessage message = new SendMessage();
		message.setParseMode("Markdown");
		message.enableMarkdown(true);
		message.disableNotification();
		message.disableWebPagePreview();
		message.setChatId(String.valueOf(chatId));
		message.setText(textToSend);


		//Keyborard
//		ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//		keyboardMarkup.setResizeKeyboard(true);
//
//		List<KeyboardRow> keyboardRows = new ArrayList<>();
//
//		KeyboardRow row = new KeyboardRow();
//		row.add("weather");
//		row.add("get random joke");
//		keyboardRows.add(row);
//
//		row = new KeyboardRow();
//		row.add("register");
//		row.add("check my data");
//		row.add("delete my data");
//		keyboardRows.add(row);
//
//		keyboardMarkup.setKeyboard(keyboardRows);
//		message.setReplyMarkup(keyboardMarkup);


		try {
			execute(message);
		} catch (TelegramApiException e) {

			log.error("Error occurred: " + e.getMessage());

		}

	}
//	addSizeCard(chatId,size,update.getMessage().getText().substring(update.getMessage().getText().indexOf("Артикул: ")+9));
private void addSizeCard(long chatId, String addSize, String article){


}

	@Scheduled(cron = "0 */10  * * * *")
	private void priceCheck(){
		var cards = cardsRepository.findAll();
		for(Cards card: cards) {
			Double currentPrice = card.getPrice();




				try {
					URL url = new URL("https://card.wb.ru/cards/detail?appType=128&curr=byn&lang=ru&dest=-59208&regions=1,4,22,30,31,33,40,48,66,68,69,70,80,83,114&spp=32&nm=" + card.getArticle() + "&uclusters=1");
					String stringUrl = "https://card.wb.ru/cards/detail?appType=128&curr=byn&lang=ru&dest=-59208&regions=1,4,22,30,31,33,40,48,66,68,69,70,80,83,114&spp=32&nm=" + card.getArticle() + "&uclusters=1";
					BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
					String line;
					StringBuilder jsonString = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						jsonString.append(line);
					}
					reader.close();
					JSONParser parser = new JSONParser();

					JSONObject rootJsonObject = (JSONObject) parser.parse(jsonString.toString());
					JSONObject data = (JSONObject) rootJsonObject.get("data");
					JSONArray products = (JSONArray) data.get("products");
					if (products.isEmpty()) {
						sendMessageNoSound(card.getChatId(), EmojiParser.parseToUnicode(":mute: Card *" + card.getProductName() + "* is not found"));
					} else {

						JSONObject product = (JSONObject) products.get(0);
						String name = (String) product.get("name");
						String brandName = (String) product.get("brand");
						Long fullPrice = (Long) product.get("salePriceU");
						Double price = fullPrice.doubleValue() / 100;
						Double rating = Double.valueOf(String.valueOf(product.get("reviewRating")));
						Long feedbackCount = (Long) product.get("feedbacks");
						JSONArray sizes = (JSONArray) product.get("sizes");
						Long stockCount = 0L;
						Long currentStock = 0L;
						String sizeName = null;
						for (int i = 0; i < sizes.size(); i++) {

							JSONObject size = (JSONObject) sizes.get(i);
							sizeName = (String) size.get("origName");

							JSONArray stocks = (JSONArray) size.get("stocks");
							if (!stocks.isEmpty()) {
								for (int j = 0; j < stocks.size(); j++) {

									JSONObject stock = (JSONObject) stocks.get(j);
									stockCount = stockCount + (Long) stock.get("qty");
									if (Objects.equals(sizeName, card.getSizeName())) {

										currentStock = currentStock + (Long) stock.get("qty");
									}


								}
							}


						}


						if (card.getSizeName() != null) {

							stockCount = currentStock;
						}

						if (stockCount == 0 && card.getStock() != 0) {
							if (card.getAmountNotification() == TRUE) {

								if (brandName.isEmpty()) {
									sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":recycle:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):recycle:\n\n" +
											":broken_heart:*Товар закончился*:broken_heart:\n\n" +
											":sparkles: Артикул: `" + card.getArticle() + "`\n\n"));
								} else {
									sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":recycle:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):recycle:\n" +
											"От бренда *" + brandName + "*\n\n" +
											":broken_heart:*Товар закончился*:broken_heart:\n\n" +
											":sparkles: Артикул: `" + card.getArticle() + "`\n\n"));
								}
							}
						}

						if (stockCount != 0 && card.getStock() == 0) {
							if (card.getAmountNotification() == TRUE) {

								if (brandName.isEmpty()) {
									sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":recycle:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):recycle:\n\n" +
											":broken_heart:*Товар в продаже*:broken_heart:\n\n" +
											":sparkles: Артикул: `" + card.getArticle() + "`\n\n"));
								} else {
									sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":recycle:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):recycle:\n" +
											"От бренда *" + brandName + "*\n\n" +
											":green_heart:*Товар в продаже*:green_heart:\n\n" +
											":sparkles: Артикул: `" + card.getArticle() + "`\n\n"));
								}

							}
						}

						if (stockCount < 10 && stockCount < card.getStock() && stockCount != 0) {
							if (card.getAmountNotification() == TRUE) {

								if (brandName.isEmpty()) {
									sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":recycle:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):recycle:\n\n" +
											":broken_heart:*Товар cкоро закончится*:broken_heart:\n\n" +
											"Осталось: *" + stockCount + "* шт.\n\n" +
											":sparkles: Артикул: `" + card.getArticle() + "`\n\n"));
								} else {
									sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":recycle:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):recycle:\n" +
											"От бренда *" + brandName + "*\n\n" +
											":broken_heart:*Товар скоро закончится*:broken_heart:\n\n" +
											"Осталось: *" + stockCount + "* шт.\n\n" +
											":sparkles: Артикул: `" + card.getArticle() + "`\n\n"));
								}

							}
						}


						if (price > currentPrice) {
							if (price - currentPrice > 0.05) {
								if (card.getPriceNotification() == TRUE) {

//						sendMessage(card.getChatId(),EmojiParser.parseToUnicode(":broken_heart: The price of the product: " + card.getProductName() + " has been increased\n\n" + currentPrice + " BYN :arrow_right: " + price + " BYN"));
									if (brandName.isEmpty()) {
										sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":diamond_shape_with_a_dot_inside:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):diamond_shape_with_a_dot_inside:\n\n" +
												":broken_heart:*Цена увеличилась*:broken_heart:\n\n" +
												"*" + currentPrice + "* BYN :arrow_right: *" + price + "* BYN \n\n" +
												":star: Рейтинг: *" + rating + "*\n" +
												":sparkles: Артикул: `" + card.getArticle() + "`\n\n" +
												":notebook: Количество отзывов: *" + feedbackCount + "*\n\n" +
												":low_brightness: Количество товара: " + stockCount));
									} else {
										sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":diamond_shape_with_a_dot_inside:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):diamond_shape_with_a_dot_inside:\n" +
												"От бренда *" + brandName + "*\n\n" +
												":broken_heart:*Цена увеличилась*:broken_heart:\n\n" +
												"*" + currentPrice + "* BYN :arrow_right: *" + price + "* BYN \n\n" +
												":star: Рейтинг: *" + rating + "*\n" +
												":sparkles: Артикул: `" + card.getArticle() + "`\n\n" +
												":notebook: Количество отзывов: *" + feedbackCount + "*\n\n" +
												":low_brightness: Количество товара: " + stockCount));
									}
								} else {
									sendMessageNoSound(card.getChatId(), EmojiParser.parseToUnicode(":mute:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + ")\n" +
											":broken_heart: *" + currentPrice + "* BYN :arrow_right: *" + price + "* BYN :broken_heart:\n\n"));
								}
							}

						} else if (price < currentPrice) {
							if (currentPrice - price > 0.05) {
								if (card.getPriceNotification() == TRUE) {

//						sendMessage(card.getChatId(),EmojiParser.parseToUnicode(":green_heart: The price of the product: " + card.getProductName() + " has been reduced\n\n" + currentPrice + " BYN :arrow_right: " + price + " BYN"));
									if (brandName.isEmpty()) {
										sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":diamond_shape_with_a_dot_inside:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):diamond_shape_with_a_dot_inside:\n\n" +
												":green_heart:*Цена уменьшилась*:green_heart:\n\n" +
												"*" + currentPrice + "* BYN :arrow_right: *" + price + "* BYN \n\n" +
												":star: Рейтинг: *" + rating + "*\n" +
												":sparkles: Артикул: `" + card.getArticle() + "`\n\n" +
												":notebook: Количество отзывов: *" + feedbackCount + "*\n\n" +
												":low_brightness: Количество товара: " + stockCount));
									} else {
										sendMessage(card.getChatId(), EmojiParser.parseToUnicode(":diamond_shape_with_a_dot_inside:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + "):diamond_shape_with_a_dot_inside:\n" +
												"От бренда *" + brandName + "*\n\n" +
												":green_heart:*Цена уменьшилась*:green_heart:\n\n" +
												"*" + currentPrice + "* BYN :arrow_right: *" + price + "* BYN \n\n" +
												":star: Рейтинг: *" + rating + "*\n" +
												":sparkles: Артикул: `" + card.getArticle() + "`\n\n" +
												":notebook: Количество отзывов: *" + feedbackCount + "*\n\n" +
												":low_brightness: Количество товара: " + stockCount));
									}
								} else {
									sendMessageNoSound(card.getChatId(), EmojiParser.parseToUnicode(":mute:[" + card.getProductName() + "](https://www.wildberries.by/product?card=" + card.getArticle() + ")\n" +
											":green_heart: *" + currentPrice + "* BYN :arrow_right: *" + price + "* BYN :green_heart:\n\n"));
								}

							}

						}
						card.setId(card.getId());
						card.setChatId(card.getChatId());
						card.setProductName(name);
						card.setBrandName(brandName);
						card.setPrice(price);
						card.setArticle(card.getArticle());
						card.setLink(stringUrl);
						card.setRating(rating);
						card.setStock(stockCount);
						card.setFeedbackCount(feedbackCount);
						cardsRepository.save(card);
						log.info("card changed: " + card.getProductName());

					}
				} catch (Exception e) {
					e.printStackTrace();
				}



			}
		}
	}



