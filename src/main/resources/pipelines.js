// Get question by indexes
db.quizzes.aggregate([
    { $match: {_id: ObjectId('5e91bedf70416b47e5db30db')}}, 
    { $unwind: "$rounds"},
    { $match: {"rounds.index": 0}},
    { $unwind: "$rounds.questions"},
    { $match: {"rounds.questions.index": 0}},
    { $group: { _id: { question: "$rounds.questions"  } }},
])

db.quizzes.aggregate([
    { "$match" : { _id : ObjectId("5e91bedf70416b47e5db30db")}}, 
    { "$unwind" : "$rounds"} , 
    { "$match" : { "rounds.index" : 0}}, 
    { "$unwind" : "$rounds.questions"}, 
    { "$match" : { "rounds.questions.index" : 0}}, 
    { "$group" : { "_id" : "$rounds.questions"}},
    { "$project": { "_id": null, "index": "$_id.index" }}
])

db.quizzes.aggregate([ 
    { "$match" : { _id : ObjectId("5e91bedf70416b47e5db30db")}} , 
    { "$unwind" : "$rounds"} , 
    { "$match" : { "rounds.index" : 0}} , 
    { "$unwind" : "$rounds.questions"} , 
    { "$match" : { "rounds.questions.index" : 0}} , 
    { "$group" : { "_id" : "$rounds.questions"}} , 
    { "$project" : { "_id" : 1 , "index" : "$_id.index" , "question" : "$_id.question" , "imageUri" : "$_id.imageUri" , "type" : "$_id.type" , "answer" : "$_id.answer" , "options" : "$_id.options"}}
])

db.quizzes.aggregate([ 
    // { "$match" : { "id" : "5e91bedf70416b47e5db30db"}} , 
    { "$match" : { _id : ObjectId("5e91bedf70416b47e5db30db")}} , 
    { "$unwind" : "$rounds"} , { "$match" : { "rounds.index" : 0}} , 
    { "$unwind" : "$rounds.questions"} , 
    { "$match" : { "rounds.questions.index" : 0}} , 
    { "$group" : { "_id" : "$rounds.questions"}} , 
    { "$project" : { "index" : "$_id.index" , "question" : "$_id.question" , "imageUri" : "$_id.imageUri" , "type" : "$_id.type" , "answer" : "$_id.answer" , "options" : "$_id.options"}}
])

db.answers.aggregate([
    { "$match" : { gameId : "5e932fb170416b4231a2fa43" }},
    { "$group" : { "_id" : "$playerId" , score: {$sum: { "$toDouble": "$score"}}}},
    { "$project": { "playerId": "$_id", "score":"$score"}}
])

db.answers.aggregate([
    "$match" : { "gameId" : "5e932fb170416b4231a2fa43"}} ,
    { "$group" : { "_id" : "$playerId" , "score" : { "$sum" : "$score"}}} ,
    { "$project" : { "playerId" : "$_id" , "score" : 1}}
]