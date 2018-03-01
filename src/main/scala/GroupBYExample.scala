
case class customer(name: String, productID: Int, qty: Int)

case class product(productID: Int, productName: String)

object GroupBYExample {

  val luckyProductsIDs = Array(1, 2, 3, 4)
  val products = List(product(1, "Iphone"), product(2, "samsung"), product(3, "MI"), product(4, "LG"))
  val customers = List(
    customer("naga", 1, 10),
    customer("john", 1, 10),
    customer("naga", 2, 2),
    customer("naga", 3, 10),
    customer("naga", 4, 1),
    customer("Kiwi", 1, 10),
    customer("Sara", 1, 10),
    customer("Kiwi", 2, 2),
    customer("Kiwi", 3, 10),
    customer("Kiwi", 4, 1)
  )


  def main(args: Array[String]): Unit = {
    val cgp = customers.groupBy(_.name).mapValues(x => {
      x.map(c => {
        c.productID
      })
    })
    val f = cgp.filter(c => {
      c._2.toArray.deep == luckyProductsIDs.deep
    })

    println(f.keys)

    // Set(Kiwi, naga) 
  }

}
