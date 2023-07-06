package com.example.pnp2_inventory_app;

import androidx.appcompat.app.AppCompatActivity;

// map and hash for testing
//import com.google.firebase.FirebaseApp;

// buttonTesting
import android.os.Bundle;

// camera and UI
import android.widget.ImageButton;
import androidx.fragment.app.Fragment;
import android.content.Intent;

// DataStructures
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

// DbConnection package!
import DbConfig.FirebaseConfig;

public class MainActivity extends AppCompatActivity {
    //this fragment is used in the camera class
    private Fragment fragment;
    // navigation object is created so we can access the navigation throughout the file
    private Navigation navigation;
    // cameraClass object allows for the camera to be called from main
    private cameraClass cameraClass;
    private fragment_home Fragment_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); //creates the instance of the program
        setContentView(R.layout.activity_main); //sets the current view to the activity
        navigation = new Navigation(this); //initialises the Navigation object
        Fragment_home = new fragment_home(); //initialises home fragment for the refresh button
        cameraClass = new cameraClass(navigation,this,this); //creates the instance of the cameraClass

        //this is the start of the implementation of the navigation bar. the code for the navigation bar is in the Navigation java file
        //The constructor takes in the context from MainActivity(this). The NavigationCreate takes in the mainActivity as the Activity(this)
        navigation.NavigationCreate(this); //if the context is not null then it will be used for the navigation bar
        if (savedInstanceState == null) {//if the saved Instance(basically the programs screen) is not active we set the home screen to the fragment being shown
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, Fragment_home).commit(); //Sets the screen to home if nothing is displayed
            navigation.GetNavigationBar().setCheckedItem(R.id.nav_home); //sets the navigation bar to having home selected
        }//this is the end of the navigation bar implementation

        SetButtons();//has to be at the bottom so the Fragments is set
    }

    private void SetButtons() {
        ImageButton RefreshBtn = this.findViewById(R.id.ImgBtnRefresh); //Intialises the refresh Button
        ImageButton ImgBtnCamera = findViewById(R.id.ImgBtnCam); //this is the camera button on the navigation bar

        RefreshBtn.setOnClickListener(v -> Fragment_home.GetItemsFromDatabase());

        ImgBtnCamera.setOnClickListener(v -> {
            cameraClass.dispatchTakePictureIntent(); //this changes the intent to a camera and gets the bitmap of the picture
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new fragment_camera()).commit(); //this changes the screen to the fragment
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cameraClass.OnActivityHelper(requestCode, resultCode, data, fragment);
        cameraClass.DetectText();
    }

    public Navigation getNavigation(){
        return navigation;
    }

    public void test(){

        // Initialize Firebase!!
        // FirebaseApp.initializeApp(this);
        // FirebaseConfig dbActions = new FirebaseConfig();

        // initialize stuff
        // dbActions.ConnectDatabase();

        // testing create method
        // Creates sample Item for testing.
        // Item item = dbActions.CreateSampleItem();

        // testing pushToDb method, pushes Item objects
        // TODO: error or null inserts handling, check the "insides" of the object before sending it
        //dbActions.InsertDb(temp);

        // getAll testing
        // TODO: gets all and it is handled in callback.
        // dbActions.GetAll("InventoryItems", new FirebaseConfig.FirestoreCallback() {
            // @Override
            // public void OnCallBack(QuerySnapshot querySnapshot) {
                // for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    // String json = document.getData().toString();
                    // List<String> test = new ArrayList<>();
                    // test.add(json);
               // }
           // }
       // });


        // GetByParameter testing
        // TODO: needs rework
        // resultList = new ArrayList<>();
        // resultList = dbActions.GetByParameterValue("Milk", "1l");

        // testing Delete
        //dbActions.DeleteFromId("ySWgoTCrn8vj5p1GG16Q");
    }
}