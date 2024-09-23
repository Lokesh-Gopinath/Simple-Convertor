package com.binary.simpleconvertor

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var inputValue: EditText
    private lateinit var convertButton: Button
    private lateinit var binaryValue: TextView
    private lateinit var signedbinary : TextView
    private lateinit var decimalValueText: TextView
    private lateinit var octalValue: TextView
    private lateinit var hexValue: TextView

    @SuppressLint("StringFormatMatches", "MissingInflatedId", "StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputValue = findViewById(R.id.inputValue)
        convertButton = findViewById(R.id.convertButton)
        signedbinary = findViewById(R.id.signedbinaryValue)
        binaryValue = findViewById(R.id.binaryValue)
        decimalValueText = findViewById(R.id.decimalValueText)
        octalValue = findViewById(R.id.octalValue)
        hexValue = findViewById(R.id.hexValue)

        convertButton.setOnClickListener {
            val input = inputValue.text.toString().trim().uppercase() // Normalize input to uppercase
            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.Default) {
                    try {
                        // Check for negative input
                        val isNegative = input.startsWith("-")
                        val absoluteInput = if (isNegative) input.substring(1) else input

                        // Validate input
                        if (!isValidInput(absoluteInput)) {
                            throw NumberFormatException("Invalid input")
                        }

                        // Determine if the input is hexadecimal
                        val decimalValue = if (absoluteInput.startsWith("0x") || absoluteInput.startsWith("0X")) {
                            convertToDecimal(absoluteInput.substring(2), 16) // Hexadecimal
                        } else {
                            convertToDecimal(absoluteInput, 10) // Decimal
                        }

                        // Apply negative sign if necessary
                        val adjustedDecimalValue = if (isNegative) -decimalValue else decimalValue

                        val binary = convertToBinary(adjustedDecimalValue)
                        val signedbin = convertToSignedBinary(adjustedDecimalValue) // Proper signed binary calculation
                        val octal = convertToOctal(adjustedDecimalValue)
                        val hex = convertToHexadecimal(adjustedDecimalValue)
                        Triple(adjustedDecimalValue, binary, Pair(signedbin, Pair(octal, hex)))
                    } catch (e: Exception) {
                        Triple(-1.0, "", null)
                    }
                }

                val (decimalValue, binary, conversions) = result
                if (decimalValue == -1.0) {
                    binaryValue.text = getString(R.string.error_invalid_input)
                    signedbinary.text = getString(R.string.error_invalid_input)
                    decimalValueText.text = ""
                    octalValue.text = ""
                    hexValue.text = ""
                } else {
                    val signedbin = conversions?.first ?: ""
                    val octal = conversions?.second?.first ?: ""
                    val hex = conversions?.second?.second ?: ""

                    // Format the decimalValue to a shorter string
                    val decimalFormat = DecimalFormat("#.##") // Format to 2 decimal places without trailing zeros
                    val formattedDecimalValue = decimalFormat.format(decimalValue).trimEnd('0').trimEnd('.')
                    binaryValue.text = getString(R.string.binary_label, binary)
                    signedbinary.text = getString(R.string.signedbinary_label, signedbin) // Set signed binary correctly
                    decimalValueText.text = getString(R.string.decimal_label, formattedDecimalValue)
                    octalValue.text = getString(R.string.octal_label, octal)
                    hexValue.text = getString(R.string.hexadecimal_label, hex)
                }
            }
        }
    }
    private fun isValidInput(input: String): Boolean {
        // Check if the input is a valid decimal or hexadecimal number
        return input.matches(Regex("^[0-9A-Fa-f]+$")) || // Hexadecimal
                input.matches(Regex("^-?[0-9]+(\\.[0-9]+)?$")) // Decimal
    }

    private fun convertToDecimal(input: String, base: Int): Double {
        if (base !in 2..36) {
            throw NumberFormatException("Invalid base. Base must be between 2 and 36.")
        }

        var isNegative = false
        var inputStr = input

        if (inputStr.startsWith("-")) {
            isNegative = true
            inputStr = inputStr.substring(1)
        }

        val parts = inputStr.split(".")
        val integerPart = parts[0]
        val fractionPart = if (parts.size > 1) parts[1] else ""

        val integerDecimal = convertIntegerPart(integerPart, base)
        val fractionDecimal = convertFractionPart(fractionPart, base)

        val decimalValue = integerDecimal + fractionDecimal
        return if (isNegative) -decimalValue else decimalValue
    }

    private fun convertIntegerPart(integerPart: String, base: Int): Double {
        var decimalValue = 0.0
        for (i in integerPart.indices) {
            val digit = integerPart[i].digitToIntOrNull(base) ?: throw NumberFormatException("Invalid digit for base $base")
            decimalValue = decimalValue * base + digit.toDouble()
        }
        return decimalValue
    }

    private fun convertFractionPart(fractionPart: String, base: Int): Double {
        var decimalValue = 0.0
        for (i in fractionPart.indices) {
            val digit = fractionPart[i].digitToIntOrNull(base) ?: throw NumberFormatException("Invalid digit for base $base")
            decimalValue += digit.toDouble() / base.toDouble().pow((i + 1).toDouble())
        }
        return decimalValue
    }


    private fun convertToBinary(decimal: Double): String {
        val isNegative = decimal < 0
        val absoluteDecimal = abs(decimal)
        val integerPart = absoluteDecimal.toInt()
        val fractionPart = absoluteDecimal - integerPart

        var binary = ""

        // Convert integer part to binary
        var num = integerPart
        while (num > 0) {
            binary = (num % 2).toString() + binary
            num /= 2
        }

        // Convert fractional part to binary with limited precision
        if (fractionPart > 0) {
            binary += "."
            var fraction = fractionPart
            for (i in 0 until 3) { // Limit to 3 fractional digits
                fraction *= 2
                val bit = fraction.toInt()
                binary += bit.toString()
                fraction -= bit
            }
        }

        // Add negative sign if the original number was negative
        return if (isNegative) "-$binary" else if (binary.isEmpty()) "0" else binary
    }

    private fun convertToSignedBinary(decimal: Double): String {
        val isNegative = decimal < 0
        val absoluteDecimal = abs(decimal)
        val integerPart = absoluteDecimal.toInt()
        val fractionPart = absoluteDecimal - integerPart

        // Convert integer part to binary
        val binaryInteger = Integer.toBinaryString(integerPart)

        // Convert fractional part to binary with limited precision (3 fractional digits)
        var binaryFraction = ""
        var fraction = fractionPart
        for (i in 0 until 3) {
            fraction *= 2
            val bit = fraction.toInt()
            binaryFraction += bit.toString()
            fraction -= bit
        }

        val binary = if (binaryFraction.isNotEmpty()) "$binaryInteger.$binaryFraction" else binaryInteger

        // If the number is negative, apply two's complement to the integer part
        return if (isNegative) {
            val twoComplement = twoComplement(binaryInteger)

            // Combine with fractional part, two's complement is only for the integer part
            if (binaryFraction.isNotEmpty()) "$twoComplement.$binaryFraction" else twoComplement
        } else {
            binary
        }
    }

    // Function to compute two's complement for a binary string
    private fun twoComplement(binary: String): String {
        // Invert the binary digits (one's complement)
        val invertedBinary = binary.map { if (it == '1') '0' else '1' }.joinToString("")

        // Add 1 to the inverted binary string to get two's complement
        return addOneToBinary(invertedBinary)
    }

    // Function to add one to a binary string (used for two's complement)
    private fun addOneToBinary(binary: String): String {
        val result = StringBuilder(binary.reversed())
        var carry = 1

        for (i in result.indices) {
            if (carry == 0) break

            when (result[i]) {
                '0' -> {
                    result.setCharAt(i, '1')
                    carry = 0
                }
                '1' -> {
                    result.setCharAt(i, '0')
                    carry = 1
                }
            }
        }

        // If carry is still 1, we need to add an extra bit at the end
        if (carry == 1) result.append('1')

        return result.reverse().toString()
    }



    private fun convertToOctal(decimal: Double): String {
        val isNegative = decimal < 0
        val absoluteDecimal = abs(decimal)
        val integerPart = absoluteDecimal.toInt()
        val fractionPart = absoluteDecimal - integerPart

        var octal = ""

        // Convert integer part to octal
        var num = integerPart
        while (num > 0) {
            octal = (num % 8).toString() + octal
            num /= 8
        }

        // Convert fractional part to octal with limited precision
        if (fractionPart > 0) {
            octal += "."
            var fraction = fractionPart
            for (i in 0 until 3) { // Limit to 3 fractional digits
                fraction *= 8
                val digit = fraction.toInt()
                octal += digit.toString()
                fraction -= digit
            }
        }

        // Add negative sign if the original number was negative
        return if (isNegative) "-$octal" else if (octal.isEmpty()) "0" else octal
    }
    private fun convertToHexadecimal(decimal: Double): String {
        val isNegative = decimal < 0
        val absoluteDecimal = abs(decimal)
        val integerPart = absoluteDecimal.toInt()
        val fractionPart = absoluteDecimal - integerPart

        val hexChars = "0123456789ABCDEF"
        var hexadecimal = ""

        // Convert integer part to hexadecimal
        var num = integerPart
        while (num > 0) {
            hexadecimal = hexChars[num % 16] + hexadecimal
            num /= 16
        }

        // Convert fractional part to hexadecimal with limited precision
        if (fractionPart > 0) {
            hexadecimal += "."
            var fraction = fractionPart
            for (i in 0 until 3) { // Limit to 3 fractional digits
                fraction *= 16
                val digit = fraction.toInt()
                hexadecimal += hexChars[digit]
                fraction -= digit
            }
        }

        // Add negative sign if the original number was negative
        return if (isNegative) "-$hexadecimal" else if (hexadecimal.isEmpty()) "0" else hexadecimal
    }
}