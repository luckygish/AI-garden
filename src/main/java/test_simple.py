#!/usr/bin/env python3
import requests
import json

# Тестируем статус API
print("🔍 Тестируем статус DeepSeek API...")
try:
    response = requests.get("http://localhost:8080/api/deepseek/status")
    print(f"Статус: {response.status_code}")
    print(f"Ответ: {response.json()}")
except Exception as e:
    print(f"Ошибка: {e}")

print("\n" + "="*50 + "\n")

# Тестируем реальный запрос
print("🧪 Тестируем реальный запрос к DeepSeek API...")
try:
    data = {
        "culture": "Помидор",
        "region": "Московская область", 
        "gardenType": "Открытый грунт"
    }
    
    response = requests.post(
        "http://localhost:8080/api/deepseek/test",
        data=data,
        headers={"Content-Type": "application/x-www-form-urlencoded"}
    )
    
    print(f"Статус: {response.status_code}")
    if response.status_code == 200:
        result = response.json()
        print("✅ Успешно получен ответ от DeepSeek API!")
        print(f"Тип ответа: {type(result)}")
        if isinstance(result, dict):
            print(f"Ключи: {list(result.keys())}")
            if 'culture' in result:
                print(f"Культура: {result['culture']}")
            if 'operations' in result:
                print(f"Количество операций: {len(result['operations'])}")
        print(f"Ответ (первые 500 символов): {str(result)[:500]}...")
    else:
        print(f"❌ Ошибка: {response.text}")
        
except Exception as e:
    print(f"Ошибка: {e}")
