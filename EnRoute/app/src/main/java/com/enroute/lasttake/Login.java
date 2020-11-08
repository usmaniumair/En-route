package com.enroute.lasttake;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class Login extends Fragment {

    private EditText email,password;
    private TextInputLayout firstName,lastName,createEmail,createPassword,confirmPassword,businessName,businessAddress,businessID;
    private Button signInButton,createAccButton,makeAccount,cancelAccount;
    private SignInButton googleSignInButton;
    private ViewFlipper loginFlipper;
    private RadioGroup accountType;
    private ConstraintLayout driverFields,managerFields;

    private GoogleSignInAccount account;
    private GoogleSignInClient signInClient;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseDatabase database;
    private StorageReference storageReference;

    private boolean googleSignUp=false,idValid=true;

    private Global global;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.login,container,false);

        global =((Global)getContext().getApplicationContext());

        email=view.findViewById(R.id.id_email);
        password=view.findViewById(R.id.id_password);
        signInButton=view.findViewById(R.id.id_signIn);
        createAccButton=view.findViewById(R.id.id_createAccount);
        cancelAccount=view.findViewById(R.id.id_cancelAccount);
        googleSignInButton=view.findViewById(R.id.id_googleSignIn);
        businessName=view.findViewById(R.id.id_businessName_layout);
        businessAddress=view.findViewById(R.id.id_businessAddress_layout);
        businessID=view.findViewById(R.id.id_businessID_layout);
        accountType=view.findViewById(R.id.id_accountType);
        driverFields=view.findViewById(R.id.id_driverFields);
        managerFields=view.findViewById(R.id.id_managerFields);
        loginFlipper=view.findViewById(R.id.id_loginFlipper);
        makeAccount=view.findViewById(R.id.id_makeAccount);
        firstName=view.findViewById(R.id.id_firstName_layout);
        lastName=view.findViewById(R.id.id_lastName_layout);
        createEmail=view.findViewById(R.id.id_createEmail_layout);
        createPassword=view.findViewById(R.id.id_create_password_layout);
        confirmPassword=view.findViewById(R.id.id_confirm_password_layout);
        loginFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.slide_from_right));
        loginFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.slide_out_to_right));

        if(global.getColorBlind()==null) {
            View colorBlindSettingView = inflater.inflate(R.layout.color_blind_setting, container, false);
            new AlertDialog.Builder(getContext())
                    .setTitle("Colorblind Setting")
                    .setView(colorBlindSettingView)
                    .setPositiveButton("Save", (dialog, which) -> {
                        RadioGroup colors = colorBlindSettingView.findViewById(R.id.id_color_options);
                        if (colors.getCheckedRadioButtonId() == R.id.id_color_normal) {
                            loginFlipper.setBackground(getResources().getDrawable(R.drawable.ic_background_normal));
                            global.setColorBlind("normal");
                        } else if (colors.getCheckedRadioButtonId() == R.id.id_color_prot) {
                            loginFlipper.setBackground(getResources().getDrawable(R.drawable.ic_background_prot));
                            global.setColorBlind("prot");
                        } else if (colors.getCheckedRadioButtonId() == R.id.id_color_deut) {
                            loginFlipper.setBackground(getResources().getDrawable(R.drawable.ic_background_deut));
                            global.setColorBlind("deut");
                        }
                        view.invalidate();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Exit", (dialog, which) -> dialog.cancel()).show();
        }else{
            global.setColorBlind(global.getColorBlind());
            if(global.getColorBlind().equals("normal"))
                loginFlipper.setBackground(getResources().getDrawable(R.drawable.ic_background_normal));
            else if(global.getColorBlind().equals("prot"))
                loginFlipper.setBackground(getResources().getDrawable(R.drawable.ic_background_prot));
            else if(global.getColorBlind().equals("deut"))
                loginFlipper.setBackground(getResources().getDrawable(R.drawable.ic_background_deut));
            view.invalidate();
        }

        accountType.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.id_driverRadio:
                    managerFields.setVisibility(View.GONE);
                    driverFields.setVisibility(View.VISIBLE);
                    break;
                case R.id.id_managerRadio:
                    driverFields.setVisibility(View.GONE);
                    managerFields.setVisibility(View.VISIBLE);
                    break;
            }
        });

        signInButton.setOnClickListener(v -> {
            if(email.getText().toString().length()>0 && password.getText().toString().length()>0)
                signIn(email.getText().toString(),password.getText().toString());
        });

        createAccButton.setOnClickListener(v -> loginFlipper.showNext());

        makeAccount.setOnClickListener(v -> {
            if(allCreateAccFieldsValid()){
                if(account==null)
                    createAccount(createEmail.getEditText().getText().toString(), createPassword.getEditText().getText().toString(),accountType.getCheckedRadioButtonId());
                else addAccountDataToDatabase(accountType.getCheckedRadioButtonId());
            }
        });

        businessID.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()==7){
                    validateBusinessID(s.toString());
                }else businessID.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        cancelAccount.setOnClickListener(v -> loginFlipper.showNext());

        googleSignInButton.setOnClickListener(v -> startActivityForResult(signInClient.getSignInIntent(), 0));

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("765389432783-17ur2256q7d7qsjv7pbqhjtjeqt82mp8.apps.googleusercontent.com")
                .requestId()
                .requestEmail()
                .requestProfile()
                .build();
        signInClient=GoogleSignIn.getClient(getContext(),gso);

        auth=MainActivity.auth;
        database=FirebaseDatabase.getInstance();
        storageReference= FirebaseStorage.getInstance().getReference();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0){
            GoogleSignIn.getSignedInAccountFromIntent(data).addOnCompleteListener(task -> {
                try {
                    account = task.getResult(ApiException.class);
                    global.setUserID(account.getId());
                    googleAccountExists(global.getUserID());
                } catch (ApiException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(),"Google Sign In Failed",Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean allCreateAccFieldsValid(){
        boolean valid=true;
        if(firstName.getEditText().getText().length()==0){
            firstName.setError("Field can't be empty");
            valid=false;
        }else firstName.setError(null);

        if(lastName.getEditText().getText().length()==0){
            lastName.setError("Field can't be empty");
            valid=false;
        }else lastName.setError(null);

        if(createEmail.getEditText().getText().length()==0 || !Patterns.EMAIL_ADDRESS.matcher(createEmail.getEditText().getText().toString()).matches()){
            createEmail.setError("Invalid email");
            valid=false;
        }else createEmail.setError(null);

        if(!googleSignUp){
            if(createPassword.getEditText().getText().length()==0){
                createPassword.setError("Field can't be empty");
                valid=false;
            }else createPassword.setError(null);

            if(confirmPassword.getEditText().getText().length()==0 || !confirmPassword.getEditText().getText().toString().equals(createPassword.getEditText().getText().toString())){
                confirmPassword.setError("Passwords do not match");
                valid=false;
            }else confirmPassword.setError(null);
        }

        if(accountType.getCheckedRadioButtonId()!=R.id.id_driverRadio && accountType.getCheckedRadioButtonId()!=R.id.id_managerRadio) {
            Toast.makeText(getContext(),"Please select an account type",Toast.LENGTH_LONG).show();
            valid=false;
        }

        if(accountType.getCheckedRadioButtonId()==R.id.id_driverRadio && businessID.getEditText().getText().length()!=7) {
            businessID.setError("Invalid business ID length");
            valid=false;
        }else businessID.setError(null);

        if(accountType.getCheckedRadioButtonId()==R.id.id_managerRadio) {
            if(businessName.getEditText().getText().length()==0){
                businessName.setError("Field can't be empty");
                valid=false;
            }else businessName.setError(null);
            if (businessAddress.getEditText().getText().length()==0){
                businessAddress.setError("Field can't be empty");
                valid=false;
            }else businessAddress.setError(null);
        }

        if(valid)
            valid=idValid;

        return valid;
    }

    private void getAccountInfo(String id){
        try {
            File idFile=File.createTempFile("user_ids","txt");
            storageReference.child("users/user_ids.txt").getFile(idFile).addOnSuccessListener(taskSnapshot -> {
                try {
                    FileInputStream inputStream=new FileInputStream(idFile);
                    BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
                    String temp_id;
                    String[] content;
                    while ((temp_id = bufferedReader.readLine()) != null) {
                        content = temp_id.split(" ");
                        if (id.equals(content[0])) {
                            global.setAccountType(content[1]);
                            global.setBusinessID(content[2]);
                            global.setName(content[3]);
                            if(global.getAccountType().equals("driver"))
                                MainActivity.showUI(new DriverNavigation());
                            else MainActivity.showUI(new ManagerContainer());
                            bufferedReader.close();
                            inputStream.close();
                            return;
                        }
                    }

                    bufferedReader.close();
                    inputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void googleAccountExists(String id){
        try {
            File idFile=File.createTempFile("user_ids","txt");
            storageReference.child("users/user_ids.txt").getFile(idFile).addOnSuccessListener(taskSnapshot -> {
                try {
                    FileInputStream inputStream=new FileInputStream(idFile);
                    BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
                    String temp_id;
                    String[] content;
                    while ((temp_id = bufferedReader.readLine()) != null) {
                        content = temp_id.split(" ");
                        if (id.equals(content[0])) {
                            global.setAccountType(content[1]);
                            global.setBusinessID(content[2]);
                            if(global.getAccountType().equals("driver"))
                                MainActivity.showUI(new DriverNavigation());
                            else MainActivity.showUI(new ManagerContainer());
                            bufferedReader.close();
                            inputStream.close();
                            return;
                        }
                    }
                    googleSignUp=true;
                    firstName.getEditText().setText(account.getGivenName());
                    lastName.getEditText().setText(account.getFamilyName()==null?"":account.getFamilyName());
                    createEmail.getEditText().setText(account.getEmail());
                    createPassword.setVisibility(View.GONE);
                    confirmPassword.setVisibility(View.GONE);
                    loginFlipper.showNext();
                    bufferedReader.close();
                    inputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void validateBusinessID(String businessID){
        try {
            File idFile=File.createTempFile("business_ids","txt");
            storageReference.child("business/business_ids.txt").getFile(idFile).addOnSuccessListener(taskSnapshot -> {
                try {
                    FileInputStream inputStream=new FileInputStream(idFile);
                    BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
                    String id;

                    while ((id = bufferedReader.readLine()) != null) {
                        if (businessID.equals(id.trim())) {
                            global.setBusinessID(businessID);
                            this.businessID.setError(null);
                            idValid=true;
                            bufferedReader.close();
                            inputStream.close();
                            return;
                        }
                    }

                    this.businessID.setError("Invalid business ID");
                    idValid=false;

                    bufferedReader.close();
                    inputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createAccount(String email,String password,int accountType){
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user = auth.getCurrentUser();
                global.setUserID(user.getUid());
                addAccountDataToDatabase(accountType);
                //Toast.makeText(getContext(),"Successful Account Creation",Toast.LENGTH_LONG).show();
            }else{
                //Toast.makeText(getContext(),"Account Creation Failed",Toast.LENGTH_LONG).show();
                task.getException().printStackTrace();
            }
        });
    }

    private void signIn(String email,String password){
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user = auth.getCurrentUser();
                global.setUserID(user.getUid());

                SharedPreferences preferences=getContext().getSharedPreferences("EnRoutePreferences",0);
                global.setBusinessID(preferences.getString("business_id_"+global.getUserID(),null));
                global.setAccountType(preferences.getString("account_type_"+global.getUserID(),null));
                if(global.getAccountType()!=null) {
                    if (global.getAccountType().equals("driver"))
                        MainActivity.showUI(new DriverNavigation());
                    else MainActivity.showUI(new ManagerContainer());
                }else{
                    getAccountInfo(global.getUserID());
                    //Toast.makeText(getContext(),"Got account info from database",Toast.LENGTH_LONG).show();
                }
                //Toast.makeText(getContext(),"Successful Login",Toast.LENGTH_LONG).show();
            }else Toast.makeText(getContext(),"Invalid Login",Toast.LENGTH_LONG).show();
        });
    }

    @SuppressLint("MissingPermission")
    private void addAccountDataToDatabase(int accountType){
        global.setName(firstName.getEditText().getText().toString()+" "+lastName.getEditText().getText().toString());
        if (accountType == R.id.id_driverRadio) {
            global.setBusinessID(businessID.getEditText().getText().toString());
            global.setAccountType("driver");
            getContext().getSharedPreferences("EnRoutePreferences",0).edit().putString("account_type_"+ global.getUserID(), global.getAccountType()).apply();
            getContext().getSharedPreferences("EnRoutePreferences",0).edit().putString("business_id_"+global.getUserID(), global.getBusinessID()).apply();
            getContext().getSharedPreferences("EnRoutePreferences",0).edit().putString("name_"+global.getUserID(), global.getName()).apply();
            try {
                File accountIDFile=File.createTempFile("user_ids",".txt");
                storageReference.child("users/user_ids.txt").getFile(accountIDFile).addOnSuccessListener(taskSnapshot -> {
                    try {
                        FileWriter fr = new FileWriter(accountIDFile,true);
                        fr.write(global.getUserID()+" "+global.getAccountType()+" "+global.getBusinessID()+" "+global.getName()+"\n");
                        fr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    storageReference.child("users/user_ids.txt").putFile(Uri.fromFile(accountIDFile));
                });
            } catch (IOException e) { e.printStackTrace(); }
            MainActivity.showUI(new DriverNavigation());
        } else if (accountType == R.id.id_managerRadio) {
            StringBuilder businessID= new StringBuilder();
            for(int x=0;x<7;x++)
                businessID.append((int) (Math.random() * 10));

            try {
                File idFile=File.createTempFile("business_ids","txt");
                storageReference.child("business/business_ids.txt").getFile(idFile)
                        .addOnSuccessListener(taskSnapshot -> {
                            try {
                                FileInputStream inputStream=new FileInputStream(idFile);
                                BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
                                String id;
                                while ((id = bufferedReader.readLine()) != null) {
                                    if (businessID.toString().equals(id.trim())) {
                                        businessID.setLength(0);
                                        for (int x = 0; x < 7; x++)
                                            businessID.append((int) (Math.random() * 9));
                                        inputStream.getChannel().position(0);
                                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                                    }
                                }
                                bufferedReader.close();
                                inputStream.close();

                                FileWriter fr=new FileWriter(idFile,true);
                                fr.write(businessID.toString()+"\n");
                                fr.close();

                                global.setBusinessID(businessID.toString());
                                global.setAccountType("manager");

                                database.getReference().child("businesses").child(businessID.toString()).child("manager").child("name").setValue(firstName.getEditText().getText().toString() + " " + lastName.getEditText().getText().toString());
                                database.getReference().child("businesses").child(businessID.toString()).child("manager").child("id").setValue(global.getUserID());
                                database.getReference().child("businesses").child(businessID.toString()).child("business_name").setValue(businessName.getEditText().getText().toString());
                                database.getReference().child("businesses").child(businessID.toString()).child("business_address").setValue(businessAddress.getEditText().getText().toString());

                                storageReference.child("business/business_ids.txt").putFile(Uri.fromFile(idFile));

                                getContext().getSharedPreferences("EnRoutePreferences",0).edit().putString("account_type_"+ global.getUserID(), global.getAccountType()).apply();
                                getContext().getSharedPreferences("EnRoutePreferences",0).edit().putString("business_id_"+global.getUserID(), global.getBusinessID()).apply();

                                try {
                                    File accountIDFile=File.createTempFile("user_ids",".txt");
                                    storageReference.child("users/user_ids.txt").getFile(accountIDFile).addOnSuccessListener(taskSnapshot1 -> {
                                        try {
                                            FileWriter fr1 = new FileWriter(accountIDFile,true);
                                            fr1.write(global.getUserID()+" "+global.getAccountType()+" "+global.getBusinessID()+" "+global.getName()+"\n");
                                            fr1.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        storageReference.child("users/user_ids.txt").putFile(Uri.fromFile(accountIDFile));
                                    });
                                } catch (IOException e) { e.printStackTrace(); }

                                MainActivity.showUI(new ManagerContainer());
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
