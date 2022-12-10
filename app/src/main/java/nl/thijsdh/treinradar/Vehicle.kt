package nl.thijsdh.treinradar

data class Vehicle(
    val treinNummer: Int,
    val ritId: String,
    val lat: Float,
    val lng: Float,
    val snelheid: Float,
    val richting: Float,
    val horizontaleNauwkeurigheid: Float,
    val type: String,
    val bron: String
)
