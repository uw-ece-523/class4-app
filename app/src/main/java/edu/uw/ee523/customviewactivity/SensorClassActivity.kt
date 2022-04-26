package edu.uw.ee523.customviewactivity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import edu.uw.ee523.customviewactivity.databinding.ActivitySensorClassBinding
import java.text.NumberFormat

class SensorClassActivity : AppCompatActivity(), SensorEventListener {

    lateinit var sensorManager: SensorManager
    lateinit var accelSensor: Sensor
    lateinit var lightSensor: Sensor

    // Related to GraphView
    private lateinit var mSeriesXaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesYaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesZaccel: LineGraphSeries<DataPoint>

    private lateinit var mGraphX: GraphView
    private lateinit var mGraphY: GraphView
    private lateinit var mGraphZ: GraphView

    val linear_acceleration: Array<Float> = arrayOf(0.0f,0.0f,0.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_sensor_class)
        var binding: ActivitySensorClassBinding = DataBindingUtil.setContentView(this, R.layout.activity_sensor_class)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        mGraphX = binding.mGraphX
        mGraphY = binding.mGraphY
        mGraphZ = binding.mGraphZ

        mSeriesXaccel = LineGraphSeries()
        mSeriesYaccel = LineGraphSeries()
        mSeriesZaccel = LineGraphSeries()
        initGraphRT(mGraphX,mSeriesXaccel!!)
        initGraphRT(mGraphY,mSeriesYaccel!!)
        initGraphRT(mGraphZ,mSeriesZaccel!!)

    }

    override fun onSensorChanged(event: SensorEvent) {
//        TODO("Not yet implemented")
        Log.i("SensorClassActivity", "Got Sensor update")
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER)
            return

        if (event.sensor.type == Sensor.TYPE_LIGHT){
            Log.i("SensorClassAct", event.values.toString())
        }

        linear_acceleration[0] = event.values[0] // X value
        linear_acceleration[1] = event.values[1] // Y value
        linear_acceleration[2] = event.values[2] // Z value

        // Update graph
        val xval = System.currentTimeMillis()/1000.toDouble()//graphLastXValue += 0.1
        mSeriesXaccel!!.appendData(DataPoint(xval, linear_acceleration[0].toDouble()), true, 50)
        mSeriesYaccel!!.appendData(DataPoint(xval, linear_acceleration[1].toDouble()), true, 50)
        mSeriesZaccel!!.appendData(DataPoint(xval, linear_acceleration[2].toDouble()), true, 50)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
//        TODO("Not yet implemented")
    }

    override fun onResume(){
        super.onResume()
        accelSensor?.also {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        lightSensor?.also {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause(){
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun initGraphRT(mGraph: GraphView, mSeriesXaccel :LineGraphSeries<DataPoint>){
        mGraph.getViewport().setXAxisBoundsManual(true)
        //mGraph.getViewport().setMinX(0.0)
        //mGraph.getViewport().setMaxX(4.0)
        mGraph.getViewport().setYAxisBoundsManual(true);

        mGraph.getViewport().setMinY(0.0);
        mGraph.getViewport().setMaxY(10.0);
        mGraph.getGridLabelRenderer().setLabelVerticalWidth(100)

        // first mSeries is a line
        mSeriesXaccel.setDrawDataPoints(false)
        mSeriesXaccel.setDrawBackground(false)
        mGraph.addSeries(mSeriesXaccel)
        setLabelsFormat(mGraph,1,2)
    }

    /* Formatting the plot*/
    fun setLabelsFormat(mGraph:GraphView,maxInt:Int,maxFraction:Int){
        val nf = NumberFormat.getInstance()
        nf.setMaximumFractionDigits(maxFraction)
        nf.setMaximumIntegerDigits(maxInt)

        mGraph.getGridLabelRenderer().setVerticalAxisTitle("Accel data")
        mGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time")

        mGraph.getGridLabelRenderer().setLabelFormatter(object : DefaultLabelFormatter(nf,nf) {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    super.formatLabel(value, isValueX)+ "s"
                } else {
                    super.formatLabel(value, isValueX)
                }
            }
        })

    }


}