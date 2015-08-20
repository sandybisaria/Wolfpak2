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


/**
 * Fragment for showing the main feed
 */
public class MainFeedFragment extends Fragment {

    /** Layouts & Buttons **/
    public ImageView refreshImageView;
    public ImageButton reportImageButton;
//    public ImageButton share;
    public RelativeLayout baseLayout;

    MainFeedNetworkingManager networkingManager = new MainFeedNetworkingManager(this);
    MainFeedLayoutManager layoutManager = new MainFeedLayoutManager(this, networkingManager);

    public int number = 0;

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

        // getActivity().setContentView(R.layout.activity_feed);

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

        /** Reference Refresh and FrameLayout **/
        refreshImageView = (ImageView) view.findViewById(R.id.main_feed_no_posts_image_view);
        baseLayout = (RelativeLayout) view.findViewById(R.id.main_feed_base_layout);

        /** Dialogs **/
        reportImageButton = (ImageButton) view.findViewById(R.id.main_feed_report_button);
//        share = (ImageButton) view.findViewById(R.id.imageButton1f);

        //getHowls() initializes the query string.
//        networkingManager.initializeRequestParams();

        /** Pull Howls from Server **/
        networkingManager.getHowls();

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

        /** Report_Button Listener **/
        reportImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                networkingManager.reportHowl();
            }
        });

        /** Refresh_Button Listener **/
        refreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                number = 0;
                layoutManager.num = 0;
                networkingManager.getHowls();
            }
        });
        super.onViewCreated(view, savedInstanceState);
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

}
