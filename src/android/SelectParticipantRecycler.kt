package src.cordova.plugin.videocall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cloud9.telehealth.R

class SelectParticipantRecycler(
  var participantDetails: List<ParticipantDetails>,
  var participantSelectListener: ParticipantSelectListener
) :
  RecyclerView.Adapter<SelectParticipantRecycler.MyHolder>() {
  class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var name = itemView.findViewById<TextView>(R.id.name)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): SelectParticipantRecycler.MyHolder {
    return MyHolder(
      LayoutInflater.from(parent.context).inflate(R.layout.participant_recycler, parent, false)
    )
  }

  override fun onBindViewHolder(holder: SelectParticipantRecycler.MyHolder, position: Int) {
    holder.name.text = when(participantDetails[position].name.contains("@")){
      true -> participantDetails[position].name.split("@")[0]
      else -> participantDetails[position].name
    }
    holder.name.setOnClickListener(View.OnClickListener {
      participantSelectListener.onParticipantSelected(position)
    })
  }

  override fun getItemCount(): Int {
    return participantDetails.size
  }

  interface ParticipantSelectListener {
    fun onParticipantSelected(position: Int)
  }
}
