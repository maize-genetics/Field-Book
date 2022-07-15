package com.fieldbook.tracker.activities

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.fieldbook.tracker.R
import com.fieldbook.tracker.brapi.BrapiControllerResponse
import com.fieldbook.tracker.brapi.model.Observation
import com.fieldbook.tracker.brapi.service.BrAPIService
import com.fieldbook.tracker.brapi.service.BrAPIServiceFactory
import com.fieldbook.tracker.brapi.service.BrapiPaginationManager
import com.fieldbook.tracker.objects.FieldObject
import com.fieldbook.tracker.objects.TraitObject
import com.fieldbook.tracker.preferences.GeneralKeys

data class StudyObservations(val traitList: MutableList<TraitObject> = mutableListOf(), val observationList: MutableList<Observation> = mutableListOf()) {
    fun merge(newStudy: StudyObservations) {
        traitList.addAll(newStudy.traitList)
        observationList.addAll(newStudy.observationList)
    }
}

class BrapiSyncObsDialog(context: Context) : Dialog(context) ,android.view.View.OnClickListener {
    private var saveBtn: Button? = null
    private var brAPIService: BrAPIService? = null

    private var studyObservations = StudyObservations()

    lateinit var paginationManager: BrapiPaginationManager

    lateinit var selectedField :FieldObject
    lateinit var fieldNameLbl : TextView
    // Creates a new thread to do importing
    private val importRunnable =
        Runnable { ImportRunnableTask(context).execute(0) }


//    constructor(context: Context) : this(context) { {
//        super(context)
//        context = context
//    }
//    fun BrapiSyncObsActivity(context: Context) {
//        super(context)
//        context = context
//    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setCanceledOnTouchOutside(false)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_brapi_sync_observations)
        brAPIService = BrAPIServiceFactory.getBrAPIService(this.context)
        val pageSize = context.getSharedPreferences("Settings", 0)
            .getString(GeneralKeys.BRAPI_PAGE_SIZE, "1000")!!.toInt()
        paginationManager = BrapiPaginationManager(0, pageSize)
        saveBtn = findViewById(R.id.brapi_save_btn)
        saveBtn!!.setOnClickListener(this)
        fieldNameLbl = findViewById(R.id.studyNameValue)
        val cancelBtn = findViewById<Button>(R.id.brapi_cancel_btn)
        cancelBtn.setOnClickListener(this)
    }
    fun setFieldObject(fieldObject: FieldObject) { this.selectedField = fieldObject}

    override fun onStart(){
        // Set our OK button to be disabled until we are finished loading
        saveBtn!!.visibility = View.GONE

        fieldNameLbl.text = selectedField.exp_name

        //need to call the load code
        loadObservations(selectedField)
    }

    override fun onClick(v: View) {
        println("Clicked: ${v.id}")
        when (v.id) {
//            R.id.brapi_save_btn -> saveObservations()
            R.id.brapi_save_btn -> saveObservations()
            R.id.brapi_cancel_btn -> dismiss()
            else -> {}
        }
        dismiss()
    }

    private fun loadObservations(study:FieldObject) {
        val brapiStudyDbId = study.exp_alias
        val fieldBookStudyDbId = study.exp_id
        val brAPIService = BrAPIServiceFactory.getBrAPIService(this.context)


        //Trying to get the traits as well:
        brAPIService!!.getTraits(brapiStudyDbId,
            { input ->
                val observationIds: MutableList<String> = ArrayList()

                for (obj in input.traits) {
                    println("Trait:" + obj.trait)
                    println("ObsIds: " + obj.externalDbId)
                    observationIds.add(obj.externalDbId)
                }

                brAPIService!!.getObservations(brapiStudyDbId, observationIds, paginationManager,
                    { obsInput ->
                        ((context as ContextWrapper).baseContext as Activity).runOnUiThread {
                        val currentStudy = StudyObservations(mutableListOf(), obsInput)
                        studyObservations.merge(currentStudy)
                        //                study.setObservations(input)
                        //                BrapiStudyDetails.merge(studyDetails, study)
                        //                println("StudyId: " + study.getStudyDbId())
                        //                println("StudyName: " + study.getStudyName())
//                        for (obs in input) {
                        for (obs in studyObservations.observationList) {
                            println("***************************")
                            println("StudyId: " + obs.studyId)
                            println("ObsId: " + obs.dbId)
                            println("UnitDbId: " + obs.unitDbId)
                            println("VariableDbId: " + obs.variableDbId)
                            println("VariableName: " + obs.variableName)
                            println("Value: " + obs.value)
                        }
                        println("Done pulling observations.")


                            makeSaveBtnVisible()
                        }

                        null
                    }) {
                    println("Stopped:")
                    null
                }

                null
            }) { null }
//        println("obsIds Size:" + observationIds.size)



    }
    fun makeSaveBtnVisible() {
        findViewById<View>(R.id.loadingPanel).visibility = View.GONE
        saveBtn!!.visibility = View.VISIBLE
    }

    fun saveObservations() {
        println(studyObservations.observationList.size)
        for(obs in studyObservations.observationList) {
            println("****************************")
            println("Saving: varName: " + obs.variableName)
            println("Saving: value: " + obs.value)
            println("Saving: studyId: " + obs.studyId)
            println("Saving: unitDBId: " + obs.unitDbId)
            println("Saving: varDbId: " + obs.variableDbId)
        }
    }

}

// Mimics the class used in the csv field importer to run the saving
// task in a different thread from the UI thread so the app doesn't freeze up.
internal class ImportRunnableTask(context: Context) : AsyncTask<Int, Int, Int>() {

    var dialog: ProgressDialog? = null
    val context = context
    var brapiControllerResponse: BrapiControllerResponse? = null
    var fail = false

    override fun onPreExecute() {
        super.onPreExecute()
        dialog = ProgressDialog(context)
        dialog!!.isIndeterminate = true
        dialog!!.setCancelable(false)
        dialog!!.setMessage(Html.fromHtml(context.resources.getString(R.string.import_dialog_importing)))
        dialog!!.show()
    }

    override fun doInBackground(vararg params: Int?): Int? {
//        try {
//            brapiControllerResponse = brAPIService.saveStudyDetails(
//                studyDetails,
//                selectedObservationLevel,
//                selectedPrimary,
//                selectedSecondary
//            )
//        } catch (e: Exception) {
//            e.printStackTrace()
//            fail = true
//        }
        return 0
    }


    override fun onPostExecute(result: Int) {

    }
}