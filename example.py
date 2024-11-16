import pandas as pd
from kafka import KafkaProducer
import json
import time

# Function to read CSV and send data
def send_data_to_kafka(csv_file, kafka_topic, bootstrap_servers):
    # Initialize Kafka producer
    producer = KafkaProducer(
        bootstrap_servers=bootstrap_servers,
        value_serializer=lambda v: json.dumps(v).encode('utf-8')  # Serialize as JSON
    )

    # Read CSV file
    df = pd.read_csv(csv_file)

    # Send each row as a message to Kafka
    for _, row in df.iterrows():
        data = row.to_dict()  # Convert row to dictionary
        producer.send(kafka_topic, value=data)
        print(f"Sent: {data}")
        time.sleep(3)

    # Close the producer
    producer.flush()
    producer.close()

if __name__ == "__main__":
    # Parameters
    csv_file = '/home/pranavmenon/Downloads/news2.csv'  # Specify the CSV file path
    kafka_topic = 'sentiment_analysis'    # Specify the Kafka topic
    bootstrap_servers = ['localhost:9092']  # Specify Kafka server(s)

    # Send the CSV data to Kafka
    send_data_to_kafka(csv_file, kafka_topic, bootstrap_servers)

