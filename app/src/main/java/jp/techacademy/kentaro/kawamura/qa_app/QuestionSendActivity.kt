package jp.techacademy.kentaro.kawamura.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream

class QuestionSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    private var mGenre: Int = 0
    private var mPictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // 渡ってきたジャンルの番号を保持する
        val extras = intent.extras //intentに追加情報としてデータを渡したいときextrasを使用する
        mGenre = extras!!.getInt("genre")//intentのキーにgenreを追加

        // UIの準備
        title = getString(R.string.question_send_title)

        sendButton.setOnClickListener(this)
        imageView.setOnClickListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //第３引数のdataにはギャラリーorカメラから返ってきた結果どちらも対応している。ただしカメラで撮った画像が入っているわけではない。入っているのはギャラリー選択した画像
        //onActivityResultのルールで、返ってきたデータはまずintent型のdataとなる。intent結果のimageを取り出したければdata(引数)の中身にあるdataを指定する。つまりdata.dataとなる。

        super.onActivityResult(requestCode, resultCode, data)   //intentの結果が出ると起動
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    //◆intentの結果が返ってこなかったにもかかわらず(intent失敗)、mPictureUriにデータが入っているならそれを削除する
                    contentResolver.delete(mPictureUri!!, null, null)   //◆2は場所 3は設定条件
                    mPictureUri = null
                }
                return
            }

            // 画像を取得
            val uri = if (data == null || data.data == null) mPictureUri else data.data
            //◆ギャラリーから選んだ場合data.dataからとる。カメラの場合data.dataに入らないのでUriを渡す.
            //つまりdataがnullのときは写真を撮ったとき。

            // URIからBitmapを取得する
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!) //◆uriをinputに展開。入力時がin
                //contentResolverのopenInputStreamでuriを分割する。データが大きいと負荷がかかるため。
                image = BitmapFactory.decodeStream(inputStream)
                //BitmapFactoryのdecodeStreamでBitmapを生成
                inputStream!!.close()//閉じる。画像ファイルは画像データが必要になる瞬間までなるべくuriで扱う
            } catch (e: Exception) {
                return
            }

            // ◆取得したBimapの長辺を500ピクセルにリサイズする　
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight)
            //500ピクセルにしたうえで小さいほうを返している。minは小さいほうを返すメソッド。

            val matrix = Matrix()//拡大縮小を行う為には、MatrixクラスのpostScaleメソッドを利用。
            matrix.postScale(scale, scale)//Canvasの左上を基点として拡大縮小を行う。左上からscaleの大きさにする

            val resizedImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)
            //◆画像の対比を変えない場合true。　画像　基点　元の幅　高さ　Matrixを指定

            // BitmapをImageViewに設定する
            imageView.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }

    override fun onClick(v: View) {
        if (v === imageView) {
            // パーミッションの許可状態を確認する
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 許可されている
                    showChooser()
                } else {
                    // 許可されていないので許可ダイアログを表示する
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)

                    return
                }
            } else {
                showChooser()
            }
        } else if (v === sendButton) {
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())
            //新たに保存場所を作ってる。contentsの下にジャンルの枠を作っている

            val data = HashMap<String, String>()

            // UID
            data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

            // タイトルと本文を取得する
            val title = titleText.text.toString()
            val body = bodyText.text.toString()

            if (title.isEmpty()) {
                // タイトルが入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.input_title), Snackbar.LENGTH_LONG).show()
                return
            }

            if (body.isEmpty()) {
                // 質問が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.question_message), Snackbar.LENGTH_LONG).show()
                return
            }

            // Preferenceから名前を取る
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val name = sp.getString(NameKEY, "")

            data["title"] = title
            data["body"] = body
            data["name"] = name!!

            // 添付画像を取得する
            val drawable = imageView.drawable as? BitmapDrawable
            //右辺のdrawableが画像のこと。imageviewに設定されている画像という意味。

            // ここでは画像データをBASE64で文字列に変換する。添付画像が設定されていれば画像を取り出してBASE64エンコードする。
            if (drawable != null) {
                val bitmap = drawable.bitmap    //drawableのなかにプロパティとしてbitmapがある。
                val baos = ByteArrayOutputStream()        //◆圧縮した画像を渡す場所。outは出力時に使う。
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                //bitmapの圧縮した画像を第３引数に書き込む。// ◆圧縮率を第2引数におく。

                val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                //圧縮した画像を64進数のバイト配列(要素がbyte型の配列)として新しく作る？
                //◆第2引数...
                data["image"] = bitmapString
            }

            genreRef.push().setValue(data, this) //画像やbodyをFirebaseに保存。//pushを使うことで一意なIDをつけたうえでsetValueを実行する。
            progressBar.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したとき
                    showChooser()
                }
                return
            }
        }
    }

    private fun showChooser() {
        // ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)//スマホにあるコンテンツをひとつ選択して、そのデータをonActivityへ返すintent
        galleryIntent.type = "image/*" //選択肢をimageに絞る
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)//さらに開くことができるファイルに限定する

        // カメラで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)//保存した画像のﾀｲﾄﾙをfilenameにする
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")//タイプはimageのjpegにする
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //MediaStore.Images.Media.EXTERNAL_CONTENT_URIは画像を保存する空間。保存する条件としてvaluesを与える。
        //ｺﾝﾃﾝﾂﾌﾟﾛﾊﾞｲﾀﾞｰを経由して、カメラで撮った画像の保存場所を決めている。その場所をmPictureUriに渡す。URIはデータの場所を表す。

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)//画像を撮るだけのintent
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)//putExtraで、カメラで撮った画像の保存先をmPictureUriにする


        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.get_image))

        // EXTRA_INITIAL_INTENTSにカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        //EXTRA_INITIAL_INTENTSに arrayOf(cameraIntent)を渡して、そのEXTRA_INITIAL_INTENTSをchooserIntentに付加する。
        //第二引数はアレイで指定するのがルール。複数インテントがあるときはコンマで追加する。例　arrayOf(cameraIntent,~,~)。今回付け足すintentはカメラだけ。

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.question_send_error_message), Snackbar.LENGTH_LONG).show()
        }
    }
}