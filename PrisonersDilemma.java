package pDilemma;

/*

Default payoff matrix (row, column) in years in prison

 \_S_|_B_
S|1,1|3,0  S = stay silent
B|0,3|2,2  B = betray other

*/

import com.pubnub.api.*;
import org.json.*;
import java.util.*;
import java.util.Scanner;

class PDGame {

	private Pubnub pubnub;
	private String channel;
	private String myUuid;

	private String myMove;
	private String opponentMove;
	private String opponentUuid;

	private boolean imReady = false;
	private boolean opponentReady = false;

	private Scanner reader;


	public PDGame(Pubnub pubnub, String channel, String uuid, Scanner reader) {
		this.pubnub = pubnub;
		this.channel = channel;
		this.myUuid = uuid;
		this.reader = reader;
	}

	/* Reads scanner for user move choice */
	public void makeChoice(Scanner reader) {
		this.reader = reader;
		System.out.println("\nDo you wish to stay silent or betray? Enter \"s\" or \"b\".");
		while (!reader.hasNext("[sb]")) {
    		System.out.println("Not correct format!");
    		reader.next();
		}
		myMove = reader.next();
		publishChoice("move", myMove);
		getOutcome();
	}

	/* Publishes message to channel based on inputs */
	public void publishChoice(String name, Object value) {
		Callback callback = new Callback() {
			public void errorCallback(String channel, PubnubError error) {
				System.out.println(error.toString());
			}
		};
		try {
			JSONObject jso = new JSONObject();
			jso.put(name, value);
			jso.put("uuid", myUuid);
			pubnub.publish(channel, jso, callback);
		} catch (JSONException e) {
			System.out.println("JSONException in publishChoice: " + e.toString());
		}
	}

	/* Gets the result of each game based on Outcome enum */
	public void getOutcome() {
		if (myMove == null || myMove.isEmpty()) {
			System.out.println("Choose your move, your opponent has made theirs.");
		} else if (opponentMove == null || opponentMove.isEmpty()) {
			System.out.println("Waiting for you opponent to choose their move");
		} else {
			for (Outcome outcome : Outcome.values()) {
				if ((myMove+opponentMove).equals(outcome.shortcut())) {
					System.out.println("\n"+outcome.memo()+"\n");
				}
			}
			closeGame();
		}
	}

	/* Subscribes to channel and handles callbacks depending on type published */
	public void subscribe() {
		Callback callback = new Callback() {		
			@Override
			public void connectCallback(String channel, Object message) {
				publishChoice("start", 1);
				imReady = true;
				System.out.println("Successfully subscribed to channel: " + channel);
				if(imReady && opponentReady) {
					System.out.println("Game begins!");
					makeChoice(reader);
				} else {
					System.out.println("Waiting for your opponent to subscribe.");
				}
			}

			@Override
			public void successCallback(String channel, Object message) {
			  	try {
			  		String tempUuid = ((JSONObject)message).get("uuid").toString();
			  		if (((JSONObject)message).has("end")) {
			  			if (!tempUuid.equals(myUuid)) {
			  				System.out.println("Your opponent has left");
			  				unsubscribe();
			  			} else {
				  			unsubscribe();
				  		}
				  	} else if (((JSONObject)message).has("reset")) {
							resetGame();
					} else if (!(tempUuid.equals(myUuid))) {
			  			if (((JSONObject)message).has("start")) {
							opponentReady = true;
							System.out.println("Game begins!");
							makeChoice(reader);
						} else {
				  			opponentMove = ((JSONObject)message).get("move").toString();
				  			opponentUuid = tempUuid;
				  			getOutcome();
			  			}	
			  		}
			  	} catch (JSONException e) {
			  		System.out.println("JSONException in callback: " + e.toString());
			  	}
			}
		};

		try {
			pubnub.subscribe(channel, callback);
		} catch (PubnubException e) {
			System.out.println(e.toString());
		}
	}	

	/* Callback checks occupancy. Allows subscriptions to this channel when occupancy < 2. 
	Exits program when occupancy > 1. */
	public void hereNow() {
		Callback callback = new Callback() {
    		public void successCallback(String channel, Object message) {
        		try {
			  		int occupancy = Integer.parseInt(((JSONObject)message).get("occupancy").toString());
			  		if (occupancy == 0) {
			  			subscribe();
			  		} else if (occupancy == 1) {
			  			opponentReady = true;
			  			subscribe();
			  		} else if (occupancy > 1) {
			  			System.out.println("Too many players in this channel. Choose another.");
			  			System.exit(0);
			  		}
			  	} catch (JSONException e) {
			  		System.out.println("JSONException hereNow: " + e.toString());
			  	}
    		}
    		public void errorCallback(String channel, PubnubError error) {
        		System.out.println(error.toString());
    		}
		};
		pubnub.hereNow(channel, callback);
	}

	/* Callback confirms successful unsubscribe from channel */
	public void unsubscribe() {
		pubnub.unsubscribe(channel);
		Callback callback = new Callback() {
    		public void successCallback(String channel, Object message) {
        		try {
			  		if (((JSONObject)message).has("uuid")) {
			  			JSONArray jsoa = ((JSONObject)message).getJSONArray("uuids");
			  			for (int i = 0 ; i < jsoa.length(); i++) {
			  				if (jsoa.getString(i).equals(myUuid)) {
			  					unsubscribe();
			  				}
			  			}
			  			endGame();
			  		} else {
			  			endGame();
			  		}
			  	} catch (JSONException e) {
			  		System.out.println("JSONException in unsubscribe: " + e.toString());
			  	}
    		}
    		public void errorCallback(String channel, PubnubError error) {
        		System.out.println(error.toString());
    		}
		};
		pubnub.hereNow(channel, callback); 
	}

	/* Closes current game and handles whether to reset or end */
	public void closeGame() {
		myMove = "";
		opponentMove = "";
		imReady = false;
		opponentReady = false;
		System.out.println("Do you wish to play again? \"y\" or \"n\"");
		while (!reader.hasNext("[yn]")) {
			System.out.println("Not correct format!");
			reader.next();
		}
		String input = reader.next();
		if (input.equals("n")) {
			publishChoice("end", 1);
		} else {
			imReady = true;
			publishChoice("reset", 1);
		}
	}

	/* Starts new game once both players have confirmed */
	public void resetGame() {
		if (imReady && opponentReady) {
			makeChoice(reader);
		} else if (imReady) {
			System.out.println("Waiting for your opponent to reset");
			opponentReady = true;
		} else {
			System.out.println("Your opponent wants to play again. \"y\" or \"n\"");
			opponentReady = true;
		}
	}
	
	/* Exit program */
	public void endGame() {
		System.exit(0);
	}
}

public class PrisonersDilemma {

	public static void main(String[] args) {
		Scanner reader = new Scanner(System.in);
		System.out.println("Enter PubNub publish key");
		String pKey = reader.nextLine();
		System.out.println("Enter PubNub subscribe key");
		String sKey = reader.nextLine();
		System.out.println("What channel would you like to join?");
		String channel = reader.nextLine();
		
		Pubnub pubnub = new Pubnub(pKey, sKey);
		String myUuid = pubnub.uuid();	

		PDGame pd = new PDGame(pubnub, channel, myUuid, reader);
		pd.hereNow();
	}
} 