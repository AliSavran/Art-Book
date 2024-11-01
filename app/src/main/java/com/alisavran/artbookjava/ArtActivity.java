package com.alisavran.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.alisavran.artbookjava.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher; //aktiviteye gidip gelme işlemi
    ActivityResultLauncher<String> permissionLauncher; //aktiviteye gitmek için izin alma işlemi
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();   //registerLauncher onCreate altında çağırılmazsa uygulama hata verir

        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);


        //verinin yeni mi oluştu yoksa önceki veriyi mi görmek istediğini ayarladığımız kod
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.equals("new")){
            //new art (yeni resim ekleme)
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.selectimage);
        }else{
            int artId = intent.getIntExtra("artId",0);
            binding.button.setVisibility(View.INVISIBLE);

            try {
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ? ", new String[]{String.valueOf(artId)}); // String değeri int değere çevirme
                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }


    }

    public void save (View view){ /*SQlite Kayıt */

        String name = binding.nameText.getText().toString();
        String artistName = binding.artistText.getText().toString();
        String year = binding.yearText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ; //sqlite a koymak için görüntüyü veriye çevirme işlemi
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray =  outputStream.toByteArray(); // veriyi byte dizisine çevirme işlemi

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts" +
                    "(id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB) ");

            String sqlString = ("INSERT INTO arts(artname, paintername, year, image) VALUES(? , ? , ? , ?)");
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1, name);
            sqLiteStatement.bindString(2, artistName);
            sqLiteStatement.bindString(3, year);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(ArtActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // İŞLEM BİTTİKTEN SONRA BÜTÜN AKTİVİTELERİ KAPATIP ANA MENÜYE DÖNER
        startActivity(intent);
    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){ // alınan görüntüyü oranlama

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height ;

        if (bitmapRatio >1){//landspace image

            width = maximumSize;
            height = (int) (width / bitmapRatio);

        }else { // portrait image
            height = maximumSize;
            height = (int) (height * bitmapRatio);

        }
        return Bitmap.createScaledBitmap(image,width,height,true);
    }



    public void selectImage (View view){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){//ANDROİD 33 plus => read_media_images

            /*depolama izni verilmemiş mi kontrolü*/
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)){//İZİN İSTEME MANTIĞINI AÇIKLAMA
                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //request permission (izin isteme)
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    }).show();
                }else {
                    //request permission (izin isteme)
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }
            }else {
                //Gallery(izin verilmiş galeriye git)
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//Galeriden görsel alma işlemi
                activityResultLauncher.launch(intentToGallery);
            }

        }else{//Android 32-- => read_external_storage

            /*depolama izni verilmemiş mi kontrolü*/
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){//İZİN İSTEME MANTIĞINI AÇIKLAMA
                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //request permission (izin isteme)
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }).show();
                }else {
                    //request permission (izin isteme)
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }else {
                //Gallery(izin verilmiş galeriye git)
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//Galeriden görsel alma işlemi
                activityResultLauncher.launch(intentToGallery);
            }
        }
    }    //İzin isteme kontrolü

    private void registerLauncher(){ // activitiyresultlauncherların tanımı
        //activityresultlauncher sonuca göre aktiviteyi başlatmasını tanımladık
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == RESULT_OK){
                    Intent intentFromResult = result.getData(); //sonuca göre veriyi alma işlemi
                    if(intentFromResult != null){ //aldığımız verinin boş olup olmadığını kontrol ediyoruz
                        Uri imageData =  intentFromResult.getData(); // kullanıcının seçtiği görselin yerini söylüyor

                        try {
                            //ImageDecoder alınan görüntüyü ilemeyi sağlar
                            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(),imageData);
                            selectedImage = ImageDecoder.decodeBitmap(source); //seçilen görüntüyü bitmape çevirme
                            binding.imageView.setImageBitmap(selectedImage);
                        }catch (Exception e){
                            e.printStackTrace(); // uygulamayı çökme olursa kullanıcıya uygulamayı çökertmeden hata mesajı verir.
                        }

                    }
                }
            }
        });
        //permission launcherın izin alma old. tanımı
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //permission granted(izin verildi)
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//Galeriden görsel alma işlemi
                    activityResultLauncher.launch(intentToGallery);
                }else{
                    //permission denied(izin verilmedi)
                    Toast.makeText(ArtActivity.this, "Permission needed !!!!!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}