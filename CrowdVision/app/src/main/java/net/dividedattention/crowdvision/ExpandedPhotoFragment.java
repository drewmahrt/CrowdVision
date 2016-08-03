package net.dividedattention.crowdvision;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by drewmahrt on 7/29/16.
 */
public class ExpandedPhotoFragment extends Fragment implements View.OnClickListener{
    private static final String IMAGE = "image";
    private static final String PHOTO_PATH = "photoPath";
    private static final String TAG = "ExpandedPhotoFragment";
    private int transitionName;
    private ImageView mFavImage;
    private TextView mLikesText;
    private String mPhotoPath;
    private User mCurrentUser;
    private Photo mCurrentPhoto;
    private boolean mPhotoLoaded;
    private boolean mUserLoaded;
    private String mPhotoKey;

    public static ExpandedPhotoFragment newInstance(Bitmap bitmap, String transitionName, String photoPath) {
        Bundle args = new Bundle();
        args.putParcelable(IMAGE,bitmap);
        args.putString("transitionName",transitionName);
        args.putString(PHOTO_PATH,photoPath);
        ExpandedPhotoFragment fragment = new ExpandedPhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.expanded_photo_fragment,container,false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            v.findViewById(R.id.image).setTransitionName(getArguments().getString("transitionName"));
        }
        mPhotoPath = getArguments().getString(PHOTO_PATH);
        String[] splitPath = mPhotoPath.split("/");
        mPhotoKey = splitPath[splitPath.length-1];
        mPhotoLoaded = false;
        mUserLoaded = false;
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bitmap bitmap = getArguments().getParcelable(IMAGE);
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        imageView.setImageBitmap(bitmap);

        mFavImage = (ImageView)view.findViewById(R.id.fav_image);
        mFavImage.setOnClickListener(this);
        mLikesText = (TextView)view.findViewById(R.id.likes_text);

        DatabaseReference photoReference = FirebaseDatabase.getInstance().getReference().child(mPhotoPath);
        photoReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCurrentPhoto = dataSnapshot.getValue(Photo.class);
                mLikesText.setText("Likes: "+mCurrentPhoto.getLikes());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fav_image:
                pressFavorite();
                break;
        }
    }

    private void pressFavorite() {
        Log.d(TAG, "pressFavorite: ");
        DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("users/"+FirebaseAuth.getInstance().getCurrentUser().getUid());
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCurrentUser = dataSnapshot.getValue(User.class);
                if(mCurrentUser == null){
                    mCurrentUser = new User();
                }
                mUserLoaded = true;
                if(mPhotoLoaded && mUserLoaded)
                    toggleLike();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference photoReference = FirebaseDatabase.getInstance().getReference().child(mPhotoPath);
        photoReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(mCurrentPhoto == null)
                    mCurrentPhoto = dataSnapshot.getValue(Photo.class);
                mPhotoLoaded = true;
                if(mPhotoLoaded && mUserLoaded)
                    toggleLike();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void toggleLike() {
        if(mPhotoLoaded && mUserLoaded){
            Log.d(TAG, "toggleLike: "+mPhotoPath);

            if(mCurrentUser.getLikesList().contains(mPhotoKey)){
                //User has already liked this photo
                Log.d(TAG,"User contained photo key "+mPhotoKey);
                mCurrentPhoto.setLikes(mCurrentPhoto.getLikes()-1);
                mCurrentUser.likesList.remove(mPhotoKey);
                mFavImage.setColorFilter(Color.argb(255,0,0,0));
            }else{
                //User hasn't liked this photo yet
                Log.d(TAG, "toggleLike: User didn't have photo key: "+mPhotoKey);
                mCurrentPhoto.setLikes(mCurrentPhoto.getLikes()+1);
                mCurrentUser.likesList.add(mPhotoKey);
                mFavImage.setColorFilter(Color.argb(255,255,51,51));
            }

            DatabaseReference userReference = FirebaseDatabase.getInstance().getReference().child("users/"+FirebaseAuth.getInstance().getCurrentUser().getUid());
            DatabaseReference photoReference = FirebaseDatabase.getInstance().getReference().child(mPhotoPath);
            userReference.setValue(mCurrentUser);
            photoReference.setValue(mCurrentPhoto);
            mLikesText.setText("Likes: "+mCurrentPhoto.getLikes());
            mPhotoLoaded = false;
            mUserLoaded = false;
        }
    }


}