from flask_sqlalchemy import SQLAlchemy

def connect_to_database(app) -> SQLAlchemy:
    app.config['SQLALCHEMY_DATABASE_URI'] = f'sqlite:///database.db'
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    db = SQLAlchemy(app)

    return db
