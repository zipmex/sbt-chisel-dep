package subproject2

object Hello {
  def apply(): Unit = {
    subproject1.Hello()
    println("hello from SubProject2 (after subproject1)")
  }
}
