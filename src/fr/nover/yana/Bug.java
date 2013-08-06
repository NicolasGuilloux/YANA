package fr.nover.yana;

import org.acra.*;
import org.acra.annotation.*;

import android.app.Application;

@ReportsCrashes(
		formKey = "",
        formUri = "https://nover.cloudant.com/acra-storage/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="broundsoupserylingestalm",
        formUriBasicAuthPassword="rVG7Ww8PrH0CSFoKLTCD1HGU"
)
public class Bug extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

          // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}