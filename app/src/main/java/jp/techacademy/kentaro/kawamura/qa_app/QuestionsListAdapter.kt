package jp.techacademy.kentaro.kawamura.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
// Adapter用レイアウトファイルから該当Ｖｉｅｗを取得
import kotlinx.android.synthetic.main.list_questions.view.*

class QuestionsListAdapter(context: Context) : BaseAdapter() {
    private var mLayoutInflater: LayoutInflater //レイアウトを取り出すメソッド？のｲﾝﾌﾚｲﾀｰを渡す変数
    private var mQuestionArrayList = ArrayList<Question>()

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        //この記述方法でｲﾝﾌﾚｲﾀｰを渡す
    }

    override fun getCount(): Int {//アダプターにセットされるデータの数を返す
        return mQuestionArrayList.size
    }

    override fun getItem(position: Int): Any {//指定ポジションのアイテムを返す
        return mQuestionArrayList[position]
    }

    override fun getItemId(position: Int): Long {//指定ポジションにつけられているIDを返す
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {//指定ポジションのView(ﾚｲｱｳﾄ)を返す
        //viewにはセットされるデータが入っている。parentは貼り付けられる側のview(ﾚｲｱｳﾄ)。今回はcontent_main。
        var convertView = view

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_questions, parent, false)
        }//nullチェックで画面いっぱいになるまでR.layout.list_questionsを作り続ける。

        val titleText = convertView!!.titleTextView as TextView
        titleText.text = mQuestionArrayList[position].title

        val nameText = convertView.nameTextView as TextView
        nameText.text = mQuestionArrayList[position].name

        val resText = convertView.resTextView as TextView
        val resNum = mQuestionArrayList[position].answers.size
        resText.text = resNum.toString()

        val bytes = mQuestionArrayList[position].imageBytes
        if (bytes.isNotEmpty()) {
     val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
            //デコードとはエンコードされたデータを元に戻すこと。decodeByteArrayの第1引数はデコードするデータを渡す。
            //第2引数にはデコードをし始める位置を指定。初めからデコードするので０としている.
            //第3引数には第2引数で指定した位置から何バイト目まで画像としてデコードするかを指定。すべてデコードしたいのでsizeで全指定

            //Bitmap.Config.ARGB_8888は、 32ビットのARGBデータでBitmapを作成する事を、示している
            //◆Mutableをtrueにしてコピーする

            val imageView = convertView.imageView as ImageView
            imageView.setImageBitmap(image)
        }

        return convertView
    }

    fun setQuestionArrayList(questionArrayList: ArrayList<Question>) {
        mQuestionArrayList = questionArrayList
        //Mainでこのメソッドを呼ぶことで、アダプターのmQuestionArrayListにFirebaseのデータを渡す。
    }
}