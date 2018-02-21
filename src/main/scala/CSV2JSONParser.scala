import java.io.{BufferedReader, FileReader}
import java.nio.file.{Path, Paths}
import org.apache.commons.csv.{CSVFormat, CSVParser}
import org.json._

class CSV2JSONParser(file:Path)  {
  val in:BufferedReader =  new BufferedReader(new FileReader(file.toString))
  lazy val records = new CSVParser(in,CSVFormat.RFC4180.withFirstRecordAsHeader)
  def csvPrint() = {
      records.forEach(r=>{
      println(r)
    })
  }
  def toJSON():JSONArray={
    var jsonRecords = new JSONArray()
    records.forEach(r=>{
      var row = new JSONObject();
      r.toMap.keySet().toArray().foreach(x=>{
        row.put(x.toString,r.get(x.toString))
      })
      jsonRecords.put(row);
    })
    //println(jsonRecords)
    jsonRecords
  }

}


object CSV2JSONParser{
  def main(args: Array[String]): Unit = {
    val csvParser = new CSV2JSONParser(Paths.get("naga.txt"))
    csvParser.toJSON()
  }
}
