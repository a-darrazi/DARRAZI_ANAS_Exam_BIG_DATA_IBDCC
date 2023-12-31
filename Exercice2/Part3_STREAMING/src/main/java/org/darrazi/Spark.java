package org.darrazi;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;

import java.util.Calendar;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

public class Spark {
    public static void main(String[] args) throws TimeoutException, StreamingQueryException {
        // Configuration Spark
        SparkConf sparkConf = new SparkConf().setAppName("ContinuousMonthlyIncidentAnalysis").setMaster("local[*]");

        // Création de la session Spark
        SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();

        // Lecture des données CSV initiales
        Dataset<Row> incidentsDF = spark.read().format("csv").option("header", true).load("incidents.csv");

        // Conversion de la colonne "date" en format de date
        incidentsDF = incidentsDF.withColumn("date", to_date(col("date"), "yyyy-MM-dd"));

        // Filtrage pour l'année en cours
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        incidentsDF = incidentsDF.filter(year(col("date")).equalTo(currentYear));

        // Création d'une colonne pour le mois
        incidentsDF = incidentsDF.withColumn("month", month(col("date")));

        // Création d'une vue temporaire pour effectuer des requêtes SQL
        incidentsDF.createOrReplaceTempView("incidents");

        // Afficher d’une manière continue l’avion ayant plus d’incidents.
        Dataset<Row> result = spark.sql("SELECT month, COUNT(*) AS incident_count " +
                "FROM incidents " +
                "GROUP BY month " +
                "ORDER BY incident_count DESC").show(2);

        // Fermeture du contexte Spark
        spark.close();
    }
}
