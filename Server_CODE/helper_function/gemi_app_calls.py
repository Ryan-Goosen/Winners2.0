from google import genai

import os


def make_gemini_api_call(argument: str, existing_categories:list):
    """
    Sends an API call to Gemini and returns a response.
    Arguments:
        - String: The question you want a response on
    Return:
         - A string from the API call made earlier
    """
    google_api_key = os.environ.get("GEMINI_API_KEY")

    if not google_api_key:
        # Handle the error if the key isn't found
        raise ValueError(
            "The GEMINI_API_KEY environment variable is not set. "
            "Please set it in your system or shell."
        )


    model_id = "gemini-2.5-flash"
    client = genai.Client(api_key=google_api_key)
    categories_str = ", ".join(existing_categories)

    prompt = f"""
    You are an expert ticket classification system. Your task is to analyze the 
    following ticket content and assign it to the SINGLE best category from the 
    list provided. If one does not exist create one.

    The valid categories are: {categories_str}

    TICKET CONTENT:
    ---
    {argument}
    ---

    INSTRUCTIONS: 
    Respond with ONLY the name of the chosen category. Do not include any 
    explanation, numbers, markdown formatting (like ```), or extra text.
    """


    return  client.models.generate_content(
        model=model_id,
        contents=prompt
    )
