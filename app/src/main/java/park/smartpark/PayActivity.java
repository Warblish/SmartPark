package park.smartpark;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.stripe.android.model.Card;
import com.stripe.android.view.CardInputWidget;

public class PayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
        Toolbar toolbar = (Toolbar) findViewById(R.id.pay_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Receive additional information from Map activity, such as how much the user actually has to pay
        //Intent intent = getIntent();
        //String message = intent.getStringExtra(Map.EXTRA_MESSAGE);
    }

    /** Called when the user presses the pay button */
    public void buttonPayPressed(View view){
        Log.wtf("status", "Pay button pressed");
        CardInputWidget mCardInputWidget = (CardInputWidget) findViewById(R.id.card_input_widget);
        Card cardToSave = mCardInputWidget.getCard();
        if (cardToSave == null) {
            //Error message occurs here
            Toast.makeText(this, "Invalid Card Data",
                    Toast.LENGTH_LONG).show();
            //mErrorDialogHandler.showError("Invalid Card Data");
        } else{
            Toast.makeText(this, "Card Accepted: Now processing",
                    Toast.LENGTH_LONG).show();
        }
        //Code for creating Stripe token and sending it to server for instant charge
    }
}
