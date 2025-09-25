# main.py

from flask import Flask, request, jsonify
from flask_sqlalchemy import SQLAlchemy
import os
import json
from datetime import datetime

from database.database_controller import create_new_ticket


# --- CONFIGURATION + SETUP ---
UPLOAD_FOLDER = 'images'
DATABASE_FILE = '/database/database.database'
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///{DATABASE_FILE}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

database = SQLAlchemy(app)
os.makedirs(UPLOAD_FOLDER, exist_ok=True)


# -- Checking File Formats --
def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS



# --- API ENDPOINT --- #
@app.route("/api/tickets", methods=["POST"])
def api_create_ticket():
    image_file = request.files.get('file')
    image_url_to_db = None
    file_path = None

    try:
        json_data = json.loads(request.form.get('data'))
    except (json.JSONDecodeError, TypeError):
        return jsonify({"error": "Invalid or missing JSON data ('data' field) in request."}), 400

    if image_file and image_file.filename and allowed_file(image_file.filename):
        try:
            unique_filename = f"{datetime.now().strftime('%Y%m%d%H%M%S')}_{filename}"
            file_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_filename)
            image_file.save(file_path)
            image_url_to_db = f"/static/ticket_images/{unique_filename}"
        except Exception as e:
            print(f"File Save Error: {e}")

    try:
        new_ticket_id = create_new_ticket(
            database=database,
            data=json_data,
            image_url=image_url_to_db
        )

        return jsonify({
            "message": "Ticket and details created successfully",
            "ticket_id": new_ticket_id,
            "image_url": image_url_to_db
        }), 201

    except ValueError as e:
        # Handle cleanup if DB insertion failed but file was saved
        if file_path and os.path.exists(file_path):
            os.remove(file_path)
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        # Handle cleanup for other unexpected errors
        if file_path and os.path.exists(file_path):
            os.remove(file_path)
        return jsonify({"error": f"An internal server error occurred: {e}"}), 500


# --- INITIALIZATION ---
if __name__ == '__main__':
    # Creates the tables defined in models.py if they don't exist
    with app.app_context():
        database.create_all()
    app.run(debug=True)