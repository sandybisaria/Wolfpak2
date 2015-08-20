package com.wolfpakapp.wolfpak2.mainfeed;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.wolfpakapp.wolfpak2.R;

import java.util.ArrayList;


/**
 * Fragment for showing the main feed
 */
public class MainFeedFragment extends Fragment {

    /** Layouts & Buttons **/
    private ImageButton reportImageButton;
//    private ImageButton share;
    private RelativeLayout baseLayout;

    MainFeedNetworkingManager networkingManager;
    MainFeedLayoutManager layoutManager;

    /** Facebook Share Features **/
//    private ShareDialog shareDialog;
//    private CallbackManager callbackManager;
//    public LoginManager manager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_feed, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
//        /** Initialize SDK & Check Security Key Hash **/
//        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
//        try {
//            PackageInfo info = getActivity().getPackageManager().getPackageInfo(
//                    "com.wolfpak.vkanitkar.wolfpakfeed",
//                    PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException ignored) {
//        }

        ImageView refreshImageView = (ImageView) view.findViewById(R.id.main_feed_no_posts_image_view);
        baseLayout = (RelativeLayout) view.findViewById(R.id.main_feed_base_layout);
        reportImageButton = (ImageButton) view.findViewById(R.id.main_feed_report_button);

        networkingManager = new MainFeedNetworkingManager(this);
        layoutManager = new MainFeedLayoutManager(this);

//        /** Facebook Share Feature **/
//        callbackManager = CallbackManager.Factory.create();
//        List<String> permissionNeeds = Arrays.asList("publish_actions");
//        manager = LoginManager.getInstance();
//        manager.logInWithPublishPermissions(getActivity(), permissionNeeds);
//        shareDialog = new ShareDialog(getActivity());
//        manager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
//            @Override
//            public void onSuccess(LoginResult loginResult) {
//                share.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View arg0) {
////                        if (howls.getVisibility() == View.VISIBLE || switch_howls.getVisibility() == View.VISIBLE){
////                            sharePicFB();
////                        } else {
////                            shareVideoFB();
////                        }
//                    }
//                });
//            }
//
//            @Override
//            public void onCancel() {
//                System.out.println("onCancel");
//            }
//
//            @Override
//            public void onError(FacebookException exception) {
//                System.out.println("onError");
//            }
//        });
        reportImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                networkingManager.reportHowl();
            }
        });

        refreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                networkingManager.getHowls();
            }
        });

        networkingManager.getHowls();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            // Ensure that the fragment is fullscreen when visible.
            getActivity().getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_FULLSCREEN);

            for (OnVisibilityChangeCallbacks callbacks : callbacksArrayList) {
                callbacks.onBecomesVisible();
            }
        } else {
            for (OnVisibilityChangeCallbacks callbacks : callbacksArrayList) {
                callbacks.onBecomesInvisible();
            }
        }
    }

    /**
     * Bring the report button to the front.
     */
    public void bringButtonsToFront() {
//        mainFeed.share.bringToFront();
        reportImageButton.bringToFront();
    }

    public RelativeLayout getBaseLayout() {
        return baseLayout;
    }

    public MainFeedNetworkingManager getNetworkingManager() {
        return networkingManager;
    }

    public MainFeedLayoutManager getLayoutManager() {
        return layoutManager;
    }

    //    /** Share Picture to Facebook **/
//    public void sharePicFB(ImageView imageView) {
//        imageView.buildDrawingCache();
//        Bitmap image = imageView.getDrawingCache();
//        SharePhoto photo = new SharePhoto.Builder()
//                .setUserGenerated(true)
//                .setBitmap(image)
//                .setCaption("#WOLFPAK2015")
//                .build();
//        SharePhotoContent content = new SharePhotoContent.Builder()
//                .addPhoto(photo)
//                .build();
//        shareDialog.show(content);
//
//    }

//    /** Share Video to Facebook **/
//    public void shareVideoFB(String url){
//        Uri mUri = Uri.parse(url);
//        try {
//            Field mUriField = VideoView.class.getDeclaredField("mUri");
//            mUriField.setAccessible(true);
//            // mUri = (Uri)mUriField.get(howls1);
//        } catch(Exception ignored) {
//        }
//
//        ShareVideo video= new ShareVideo.Builder()
//                .setLocalUrl(mUri) //alwaysNull - Check later
//                .build();
//        ShareVideoContent content = new ShareVideoContent.Builder()
//                .setVideo(video)
//                .build();
//
//        shareDialog.show(content);
//    }

//    @Override
//    /**Facebook ReInitializer **/
//    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        callbackManager.onActivityResult(requestCode, resultCode, data);
//    }

    interface OnVisibilityChangeCallbacks {
        /**
         * Callback to be invoked when the fragment becomes visible.
         */
        void onBecomesVisible();

        /**
         * Callback to be invoked when the fragment loses visibility.
         */
        void onBecomesInvisible();
    }

    private ArrayList<OnVisibilityChangeCallbacks> callbacksArrayList = new ArrayList<>();

    public void addCallbacks(OnVisibilityChangeCallbacks callbacks) {
        callbacksArrayList.add(callbacks);
    }
}
