package jp.techacademy.kentaro.kawamura.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.list_question_detail.view.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class QuestionDetailListAdapter(context: Context, private val mQustion: Question) : BaseAdapter(){
    companion object {//どのレイアウトを使って表示させるかを判断するためのタイプを表す定数を用意する。
        private val TYPE_QUESTION = 0
        private val TYPE_ANSWER = 1
    }







    private var mLayoutInflater: LayoutInflater? = null//private var mLayoutInflater: LayoutInflaterでもおｋ

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return 1 + mQustion.answers.size//質問1個＋質問の数
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {//作ったレイアウトのポジションが0なら(1番上なら)TYPE_QUESTIONを返す
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }

    override fun getViewTypeCount(): Int {
        return 2//view(レイアウト）のタイプの数を返す
    }

    override fun getItem(position: Int): Any {
        return mQustion
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view



        if (getItemViewType(position) == TYPE_QUESTION) {
            if (convertView == null) {
                convertView = mLayoutInflater!!.inflate(R.layout.list_question_detail, parent, false)!!//LayoutInflater? = nullで定義しているから!!をつける。
            }

            val body = mQustion.body//このクラスの引数としてとってきたデータからbodyを取り出す
            val name = mQustion.name

            val bodyTextView = convertView.bodyTextView as TextView
            bodyTextView.text = body

            val nameTextView = convertView.nameTextView as TextView
            nameTextView.text = name

            val bytes = mQustion.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
                imageView.setImageBitmap(image)
            }
        } else {
            if (convertView == null) {
                convertView = mLayoutInflater!!.inflate(R.layout.list_answer, parent, false)!!
            }

            val answer = mQustion.answers[position - 1]//質問分を-１する。1個目のviewは質問で使っているため。
            val body = answer.body
            val name = answer.name

            val bodyTextView = convertView.bodyTextView as TextView
            bodyTextView.text = body

            val nameTextView = convertView.nameTextView as TextView
            nameTextView.text = name
        }

        return convertView
    }
}

