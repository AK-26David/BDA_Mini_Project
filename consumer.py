from kafka import KafkaConsumer
from sklearn.feature_extraction.text import TfidfVectorizer
import joblib
import pandas as pd
import json

def consume_data_from_kafka(kafka_topic, bootstrap_servers, group_id=None):
    consumer = KafkaConsumer(
        kafka_topic,
        bootstrap_servers=bootstrap_servers,
        auto_offset_reset='earliest',
        enable_auto_commit=True,
        value_deserializer=lambda v: v.decode('utf-8')  # Raw string, no JSON decoding yet
    )

    print(f"Listening for messages on topic '{kafka_topic}'...")

    for message in consumer:
        if message.value:  # Check if message is not empty
            try:
                data = json.loads(message.value) 
                original = pd.read_csv("/home/pranavmenon/Downloads/news1.csv") # Try to parse as JSON
                #print(f"Received: {data}")
                new_data = [data["Title"]]
                tf = joblib.load('tfidf_vectorizer.pkl')
                vect = pd.DataFrame(tf.transform(new_data).toarray())
                new_data = pd.DataFrame(vect)
                clf = joblib.load("/home/pranavmenon/bda_project/model_new.pkl")
                pred = clf.predict(new_data)
                print(f"Title: {data['Title']}\nPrediction: {pred}")
            except json.JSONDecodeError as e:
                print(f"JSON Decode Error: {e} - Raw message: {message.value}")
        else:
            print("Received an empty message")

if __name__ == "__main__":
    kafka_topic = 'sentiment_analysis'
    bootstrap_servers = ['localhost:9092']
    group_id = 'your_consumer_group'

    consume_data_from_kafka(kafka_topic, bootstrap_servers, group_id)

