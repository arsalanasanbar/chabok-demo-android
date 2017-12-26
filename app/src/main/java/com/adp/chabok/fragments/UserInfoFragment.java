package com.adp.chabok.fragments;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.adp.chabok.R;
import com.adp.chabok.activity.IntroActivity;
import com.adp.chabok.common.Constants;
import com.adp.chabok.common.Validator;
import com.adp.chabok.ui.Button;
import com.adp.chabok.ui.EditText;
import com.adp.chabok.ui.OnCustomListener;
import com.adpdigital.push.AdpPushClient;

import java.util.HashMap;


public class UserInfoFragment extends Fragment {

    private View view;
    private EditText fullName;
    private EditText contactInfo;
    private ImageView avatar;
    private int avatarId;
    private LinearLayout registerLayout;
    private boolean isLogoZeroScaled = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_user_info, container, false);
        initView();
        return view;
    }

    private void initView() {
        Typeface lightTypeface = Typeface.createFromAsset(getContext().getAssets(), Constants.APPLICATION_LIGHT_FONT);


        avatar = view.findViewById(R.id.avatar);
        registerLayout = view.findViewById(R.id.register_layout);

        fullName = view.findViewById(R.id.full_name);
        fullName.setTypeface(lightTypeface);

        contactInfo = view.findViewById(R.id.contact_info);
        contactInfo.setTypeface(lightTypeface);


        final Animation scaleAnimation = new ScaleAnimation(
                1f, 0f, // Start and end values for the X axis scaling
                1f, 0f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        scaleAnimation.setFillAfter(true); // Needed to keep the result of the animation
        scaleAnimation.setDuration(1000);
        scaleAnimation.setInterpolator(new AccelerateInterpolator());


        contactInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isLogoZeroScaled) {
                    avatar.startAnimation(scaleAnimation);
                    registerLayout.animate().translationY(-(avatar.getHeight() + 25)).setDuration(1200);
                    isLogoZeroScaled = true;

                }
            }
        });

        fullName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isLogoZeroScaled) {
                    avatar.startAnimation(scaleAnimation);
                    registerLayout.animate().translationY(-avatar.getHeight()).setDuration(1200);
                    isLogoZeroScaled = true;

                }

            }
        });

        contactInfo.setCustomEventListener(new OnCustomListener() {
            public void onEvent() {

                scaleBackLogo();
            }

        });

        fullName.setCustomEventListener(new OnCustomListener() {
            public void onEvent() {

                scaleBackLogo();
            }

        });


        Bundle bundle = getArguments();
        if (bundle != null) {

            avatarId = bundle.getInt(IntroActivity.AVATAR_ID);
            ImageView avatar = view.findViewById(R.id.avatar);
            avatar.setBackgroundResource(avatarId);

        }

        Button letsGo = view.findViewById(R.id.lets_go_btn);
        letsGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isLogoZeroScaled) {
                    scaleBackLogo();
                }

                if (Validator.validateNotNull(getActivity(), fullName.getText().toString(), R.string.full_name) &&
                        Validator.validateName(getActivity(), fullName.getText().toString()) &&
                        Validator.validateNotNull(getActivity(), contactInfo.getText().toString(), R.string.contact_info) &&
                        Validator.validatePhoneNumber(getActivity(), contactInfo.getText().toString())) {


                    SharedPreferences myPref = PreferenceManager.getDefaultSharedPreferences(getActivity());


                    String pushClientUserId = AdpPushClient.get().getUserId();
                    if (pushClientUserId == null || pushClientUserId.isEmpty()) {

                        AdpPushClient client = AdpPushClient.get();
                        String userId = Validator.createUserId(contactInfo.getText().toString());


                        SharedPreferences.Editor editor = myPref.edit();
                        editor.putString(Constants.PREFERENCE_NAME, fullName.getText().toString());
                        editor.apply();

                        HashMap<String, Object> userInfo = new HashMap<>();
                        userInfo.put("name", fullName.getText().toString());
                        userInfo.put("avatarIdx", getAvatarIndex(avatarId));
                        userInfo.put("userId", userId);
                        client.setUserInfo(userInfo);

                        client.register(userId, new String[]{Constants.CHANNEL_NAME, Constants.CAPTAIN_CHANNEL_NAME});

                        ((IntroActivity) getActivity()).gotToMain();


                    }

                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        fullName.requestFocus();

    }

    private void scaleBackLogo() {
        Animation scaleAnimation = new ScaleAnimation(
                0f, 1f, // Start and end values for the X axis scaling
                0f, 1f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        scaleAnimation.setFillAfter(true); // Needed to keep the result of the animation
        scaleAnimation.setDuration(1000);
        scaleAnimation.setInterpolator(new AccelerateInterpolator());
        avatar.startAnimation(scaleAnimation);

        registerLayout.animate().translationY(0).setDuration(1200);
        isLogoZeroScaled = false;

    }


    private int getAvatarIndex(int id) {
        switch (id) {
            case R.drawable.brown_selected:
                return 0;

            case R.drawable.red_selected:
                return 1;

            case R.drawable.white_selected:
                return 2;

            case R.drawable.gold_selected:
                return 3;

            default:
                return 0;
        }
    }

}
